package com.clausify.analysis;

import com.clausify.config.AsyncConfig;
import com.clausify.contract.Contract;
import com.clausify.contract.ContractRepository;
import com.clausify.contract.ContractStatus;
import com.clausify.contract.ContractUploadedEvent;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs contract analysis in the background and moves the contract through its lifecycle (FR-5):
 * UPLOADED → ANALYZING → ANALYZED, or FAILED with a reason the user can read.
 *
 * <p>One analysis has three steps, and only the first and last touch the database:
 * <ol>
 *   <li><b>Claim</b> (short transaction): lock the row, move UPLOADED → ANALYZING, read the text.</li>
 *   <li><b>Analyze</b> (no transaction): call the {@link ContractAnalyzer}, which can take minutes. Holding a
 *       database connection that long would drain the small connection pool, so no transaction is open here.</li>
 *   <li><b>Save</b> (short transaction): store the analysis and findings, then ANALYZING → ANALYZED. Any failure
 *       instead stores FAILED with a reason.</li>
 * </ol>
 *
 * <p>Assumes a single API instance (Render's free tier). Startup recovery treats every ANALYZING contract as
 * abandoned, which is only true when no other instance is working on it. Running several instances would need a
 * lease: a "claimed at" timestamp that other instances respect until it expires.
 */
@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    static final String BUSY = "Too many contracts are being analyzed right now. Please retry in a few minutes.";
    static final String TIMED_OUT = "The analysis took too long and was stopped. Please retry.";
    static final String RESTARTED = "The analysis was interrupted by a server restart. Please retry.";
    static final String UNEXPECTED = "The analysis failed unexpectedly. Please retry.";

    private final ContractRepository contracts;
    private final AnalysisRepository analyses;
    private final ContractAnalyzer analyzer;
    private final RiskScorer scorer;
    private final AnalysisProperties properties;
    private final TaskExecutor executor;
    private final TransactionTemplate newTransaction;
    private final Clock clock;

    public AnalysisService(ContractRepository contracts, AnalysisRepository analyses, ContractAnalyzer analyzer,
                           RiskScorer scorer, AnalysisProperties properties,
                           @Qualifier(AsyncConfig.ANALYSIS_EXECUTOR) TaskExecutor executor,
                           PlatformTransactionManager transactionManager, Clock clock) {
        this.contracts = contracts;
        this.analyses = analyses;
        this.analyzer = analyzer;
        this.scorer = scorer;
        this.properties = properties;
        this.executor = executor;
        // REQUIRES_NEW: the upload listener below runs while the upload's transaction is finishing. Spring's rule
        // for that phase is that new database work must start its own transaction, or it may silently join the one
        // that has already committed.
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
    }

    /**
     * Queues analysis once the upload (or retry) has committed. AFTER_COMMIT matters: the worker thread reads the
     * contract with its own connection, and a row that is not committed yet would be invisible to it.
     *
     * <p>This is a plain {@code execute} call rather than {@code @Async}, because {@code @Async} would throw a
     * full-queue rejection back into the finished upload request, and the contract would sit in UPLOADED forever.
     * Here the rejection is caught and recorded as a FAILED status.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onContractUploaded(ContractUploadedEvent event) {
        submit(event.contractId());
    }

    /**
     * A restart (a deploy, or Render recycling the instance) loses the in-memory queue and stops any running
     * analysis. Contracts it interrupted are failed with a retryable reason. They are not retried automatically,
     * because the contract itself may be what crashed the server. Uploads that never started are queued again.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (properties.recoverOnStartup()) {
            recoverInterruptedAnalyses();
        }
    }

    void recoverInterruptedAnalyses() {
        List<Long> interrupted = contracts.findIdsByStatus(ContractStatus.ANALYZING);
        List<Long> waiting = contracts.findIdsByStatus(ContractStatus.UPLOADED);
        if (!interrupted.isEmpty() || !waiting.isEmpty()) {
            log.info("Startup recovery: failing {} interrupted analyses, queueing {} waiting uploads",
                    interrupted.size(), waiting.size());
        }
        interrupted.forEach(id -> fail(id, RESTARTED));
        waiting.forEach(this::submit);
    }

    void submit(Long contractId) {
        try {
            executor.execute(() -> run(contractId));
        } catch (TaskRejectedException e) {
            log.warn("Analysis queue is full; contract {} fails as busy", contractId);
            fail(contractId, BUSY);
        }
    }

    /** One analysis, start to finish. Never throws: the worker thread has nobody to throw to. */
    void run(Long contractId) {
        try {
            Optional<String> text = claim(contractId);
            if (text.isEmpty()) {
                return;
            }
            long started = System.nanoTime(); // a monotonic clock, unaffected by wall-clock adjustments
            ContractAnalysisResult result = analyzeWithTimeout(text.get());
            save(contractId, result, Duration.ofNanos(System.nanoTime() - started));
        } catch (ContractAnalysisException e) {
            // Its message is written for users (rate limits, invalid replies, too long, timeouts).
            log.warn("Analysis of contract {} failed: {}", contractId, e.getMessage());
            fail(contractId, e.getMessage());
        } catch (RuntimeException e) {
            // Anything else is a bug or an infrastructure fault. Its message could hold internals, so the user
            // gets a generic reason and the details go to the log.
            log.error("Analysis of contract {} failed unexpectedly", contractId, e);
            fail(contractId, UNEXPECTED);
        }
    }

    /** UPLOADED → ANALYZING, returning the text to analyze, or empty if this contract should not be analyzed. */
    private Optional<String> claim(Long contractId) {
        return newTransaction.execute(status -> {
            Optional<Contract> found = contracts.findLockedById(contractId);
            if (found.isEmpty()) {
                log.info("Contract {} was deleted before analysis started", contractId);
                return Optional.<String>empty();
            }
            Contract contract = found.get();
            if (contract.getStatus() != ContractStatus.UPLOADED) {
                // Queued twice (say, a retry plus startup recovery); the first worker already took it.
                log.info("Contract {} is {}, not UPLOADED; skipping", contractId, contract.getStatus());
                return Optional.<String>empty();
            }
            contract.markAnalyzing();
            return Optional.of(contract.getExtractedText());
        });
    }

    /**
     * Runs the analyzer on its own virtual thread and waits at most {@code clausify.analysis.timeout}. On timeout
     * the call is interrupted, which stops a rate-limit wait at once. A Groq request already in flight ends on its
     * own HTTP timeout (60 seconds); its result is ignored because the contract is already FAILED.
     */
    private ContractAnalysisResult analyzeWithTimeout(String text) {
        FutureTask<ContractAnalysisResult> call = new FutureTask<>(() -> analyzer.analyze(text));
        Thread.ofVirtual().name("analysis-call").start(call);
        try {
            ContractAnalysisResult result = call.get(properties.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (result == null) {
                throw new IllegalStateException("Analyzer returned no result");
            }
            return result;
        } catch (TimeoutException e) {
            call.cancel(true);
            throw new ContractAnalysisException(TIMED_OUT, e);
        } catch (InterruptedException e) {
            call.cancel(true);
            Thread.currentThread().interrupt();
            throw new ContractAnalysisException(RESTARTED, e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ContractAnalysisException analysisFailure) {
                throw analysisFailure;
            }
            throw new IllegalStateException("Analyzer failed", e.getCause());
        }
    }

    /** Stores the results and marks the contract ANALYZED, unless it was deleted in the meantime. */
    private void save(Long contractId, ContractAnalysisResult result, Duration elapsed) {
        RiskScorer.RiskScore score = scorer.score(result.findings());
        newTransaction.executeWithoutResult(status -> {
            Optional<Contract> found = contracts.findLockedById(contractId);
            if (found.isEmpty()) {
                log.info("Contract {} was deleted during analysis; discarding the result", contractId);
                return;
            }
            Contract contract = found.get();
            Analysis analysis = new Analysis();
            analysis.setContract(contract);
            analysis.setOverallScore(score.overall());
            analysis.setRiskLevel(score.level());
            analysis.setContractType(result.contractType());
            analysis.setSummary(result.summary());
            analysis.setModelName(analyzer.modelName());
            analysis.setDurationMs(elapsed.toMillis());
            result.findings().forEach(finding -> analysis.addFinding(toEntity(finding)));
            analyses.save(analysis);
            contract.markAnalyzed(clock.instant());
        });
        log.info("Analyzed contract {} in {} ms: {} findings, score {} ({})", contractId, elapsed.toMillis(),
                result.findings().size(), score.overall(), score.level());
    }

    /** Records FAILED with a reason, if the contract still exists and is still in progress. */
    private void fail(Long contractId, String reason) {
        try {
            newTransaction.executeWithoutResult(status -> contracts.findLockedById(contractId)
                    .filter(c -> c.getStatus() == ContractStatus.UPLOADED || c.getStatus() == ContractStatus.ANALYZING)
                    .ifPresent(c -> c.markFailed(reason)));
        } catch (RuntimeException e) {
            // The database itself is failing. Startup recovery will fail this contract later.
            log.error("Could not mark contract {} as failed", contractId, e);
        }
    }

    private static ClauseFinding toEntity(ContractAnalysisResult.Finding finding) {
        ClauseFinding entity = new ClauseFinding();
        entity.setCategory(finding.category());
        entity.setRiskScore(finding.riskScore());
        entity.setRiskLevel(RiskLevel.fromScore(finding.riskScore()));
        entity.setFavorable(finding.favorable());
        entity.setTitle(finding.title());
        entity.setSectionRef(finding.sectionRef());
        entity.setOriginalText(finding.originalText());
        entity.setExplanation(finding.explanation());
        entity.setSuggestedRevision(finding.suggestedRevision());
        return entity;
    }
}
