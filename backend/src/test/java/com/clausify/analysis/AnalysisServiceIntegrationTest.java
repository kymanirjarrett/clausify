package com.clausify.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clausify.Polling;
import com.clausify.TestcontainersConfiguration;
import com.clausify.contract.Contract;
import com.clausify.contract.ContractRepository;
import com.clausify.contract.ContractStatus;
import com.clausify.contract.ContractUploadedEvent;
import com.clausify.user.User;
import com.clausify.user.UserRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The analysis pipeline against real MySQL, with the real event, transaction, and executor wiring. Only the
 * analyzer is a mock, so each test can make the "AI" succeed, fail, hang, or block on demand. No network calls.
 *
 * <p>One worker and a queue of one make "busy" reachable with three uploads; a 3-second timeout keeps the timeout
 * test short. Startup recovery is off so the test calls it explicitly.
 */
@SpringBootTest(properties = {
        "clausify.analysis.worker-threads=1",
        "clausify.analysis.queue-capacity=1",
        "clausify.analysis.timeout=3s",
        "clausify.analysis.recover-on-startup=false"})
@Import(TestcontainersConfiguration.class)
class AnalysisServiceIntegrationTest {

    @MockitoBean
    private ContractAnalyzer analyzer;

    @Autowired
    private AnalysisService service;

    @Autowired
    private ContractRepository contracts;

    @Autowired
    private AnalysisRepository analyses;

    @Autowired
    private ClauseFindingRepository findings;

    @Autowired
    private UserRepository users;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;
    private User owner;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        String email = "analysis-" + UUID.randomUUID() + "@example.com";
        owner = users.save(User.builder().name("Analyst").email(email).password("hash").build());
        when(analyzer.modelName()).thenReturn("test-model");
        when(analyzer.analyze(anyString())).thenReturn(fourFindings());
    }

    @Test
    void successfulAnalysisStoresTheScoreAndFindings() {
        Long id = upload("Section 7.2: liable without limitation as to amount or type.");

        Contract done = awaitStatus(id, ContractStatus.ANALYZED);
        assertThat(done.getAnalyzedAt()).isNotNull();
        assertThat(done.getFailureReason()).isNull();

        Analysis analysis = analyses.findByContractId(id).orElseThrow();
        // FR-5 example 3, worked by hand: the favorable 20 is skipped, so highest = 85 and the average of
        // 85, 78, 55 = 72.67. 0.6 × 85 + 0.4 × 72.67 = 80.07, which rounds to 80: High.
        assertThat(analysis.getOverallScore()).isEqualTo(80);
        assertThat(analysis.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(analysis.getModelName()).isEqualTo("test-model");
        assertThat(analysis.getContractType()).isEqualTo("Consulting agreement");
        assertThat(analysis.getDurationMs()).isNotNegative();

        List<ClauseFinding> stored = findings.findByAnalysisId(analysis.getId()).stream()
                .sorted(Comparator.comparing(ClauseFinding::getRiskScore).reversed()).toList();
        assertThat(stored).extracting(ClauseFinding::getRiskScore).containsExactly(85, 78, 55, 20);
        assertThat(stored).extracting(ClauseFinding::getRiskLevel)
                .containsExactly(RiskLevel.HIGH, RiskLevel.HIGH, RiskLevel.MEDIUM, RiskLevel.LOW);
        // FR-5 example 1: the liability finding keeps its quote, explanation, and revision.
        assertThat(stored.get(0).getCategory()).isEqualTo(RiskCategory.LIABILITY);
        assertThat(stored.get(0).getOriginalText()).contains("without limitation");
        assertThat(stored.get(0).getSuggestedRevision()).contains("cap");
        // FR-5 example 2: the favorable payment clause is stored as favorable and Low.
        assertThat(stored.get(3).isFavorable()).isTrue();
        assertThat(stored.get(3).getSuggestedRevision()).isNull();
    }

    @Test
    void analyzerFailureMarksTheContractFailedWithItsReason() {
        when(analyzer.analyze("rate limited")).thenThrow(
                new ContractAnalysisException("The AI service's usage limit has been reached. Please retry later."));

        Long id = upload("rate limited");

        // FR-5 example 4.
        Contract failed = awaitStatus(id, ContractStatus.FAILED);
        assertThat(failed.getFailureReason())
                .isEqualTo("The AI service's usage limit has been reached. Please retry later.");
        assertThat(failed.isRetryable()).isTrue();
        assertThat(analyses.findByContractId(id)).isEmpty();
    }

    @Test
    void unexpectedErrorGivesAGenericReasonWithoutInternals() {
        when(analyzer.analyze("bug")).thenThrow(new IllegalStateException("jdbc:mysql://internal-host password=x"));

        Contract failed = awaitStatus(upload("bug"), ContractStatus.FAILED);

        assertThat(failed.getFailureReason()).isEqualTo(AnalysisService.UNEXPECTED);
    }

    @Test
    void hungAnalyzerIsStoppedAtTheTimeout() throws InterruptedException {
        CountDownLatch neverReleased = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        when(analyzer.analyze("hangs")).thenAnswer(call -> {
            try {
                neverReleased.await(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                interrupted.set(true); // the timeout interrupted the call, so a rate-limit wait would stop too
                throw e;
            }
            return fourFindings();
        });

        Contract failed = awaitStatus(upload("hangs"), ContractStatus.FAILED);

        assertThat(failed.getFailureReason()).isEqualTo(AnalysisService.TIMED_OUT);
        Polling.await(interrupted::get, Boolean::booleanValue);
    }

    @Test
    void fullQueueFailsNewUploadsAsBusy() throws InterruptedException {
        CountDownLatch release = new CountDownLatch(1);
        when(analyzer.analyze("first")).thenAnswer(call -> {
            release.await(10, TimeUnit.SECONDS);
            return fourFindings();
        });
        try {
            Long first = upload("first");
            awaitStatus(first, ContractStatus.ANALYZING); // the only worker is busy
            Long second = upload("second");               // fills the queue of one
            Long third = upload("third");                 // nowhere to go

            // Rejected synchronously when the upload committed, so FAILED is already stored.
            assertThat(contracts.findById(third).orElseThrow().getStatus()).isEqualTo(ContractStatus.FAILED);
            assertThat(contracts.findById(third).orElseThrow().getFailureReason()).isEqualTo(AnalysisService.BUSY);

            release.countDown();
            awaitStatus(first, ContractStatus.ANALYZED);
            awaitStatus(second, ContractStatus.ANALYZED);
        } finally {
            release.countDown();
        }
    }

    @Test
    void contractDeletedDuringAnalysisDiscardsTheResult() throws InterruptedException {
        CountDownLatch release = new CountDownLatch(1);
        when(analyzer.analyze("deleted midway")).thenAnswer(call -> {
            release.await(10, TimeUnit.SECONDS);
            return fourFindings();
        });
        try {
            Long id = upload("deleted midway");
            awaitStatus(id, ContractStatus.ANALYZING);
            contracts.deleteById(id); // FR-8 while the AI is still working
            release.countDown();

            // One worker: once a later upload is analyzed, the deleted one has been handled.
            awaitStatus(upload("after"), ContractStatus.ANALYZED);
            assertThat(contracts.findById(id)).isEmpty();
            assertThat(analyses.findByContractId(id)).isEmpty();
        } finally {
            release.countDown();
        }
    }

    @Test
    void sameContractQueuedTwiceIsAnalyzedOnce() {
        Long id = tx.execute(status -> {
            Long saved = contracts.save(contract("queued twice")).getId();
            events.publishEvent(new ContractUploadedEvent(saved));
            events.publishEvent(new ContractUploadedEvent(saved));
            return saved;
        });

        awaitStatus(id, ContractStatus.ANALYZED);
        awaitStatus(upload("after duplicate"), ContractStatus.ANALYZED); // the duplicate task has run by now

        verify(analyzer, times(1)).analyze("queued twice");
    }

    @Test
    void startupRecoveryFailsInterruptedAnalysesAndQueuesWaitingUploads() {
        Long interrupted = tx.execute(status -> {
            Contract contract = contracts.save(contract("was running"));
            contract.markAnalyzing(); // as if the server stopped mid-analysis
            return contract.getId();
        });
        Long waiting = contracts.save(contract("never started")).getId(); // no event: its queue entry was lost

        service.recoverInterruptedAnalyses();

        Contract failed = contracts.findById(interrupted).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(ContractStatus.FAILED);
        assertThat(failed.getFailureReason()).isEqualTo(AnalysisService.RESTARTED);
        awaitStatus(waiting, ContractStatus.ANALYZED);
        verify(analyzer, times(0)).analyze("was running"); // not retried automatically
    }

    /** Saves a contract and publishes the upload event in one transaction, exactly like ContractService.upload. */
    private Long upload(String text) {
        return tx.execute(status -> {
            Long id = contracts.save(contract(text)).getId();
            events.publishEvent(new ContractUploadedEvent(id));
            return id;
        });
    }

    private Contract contract(String text) {
        return Contract.uploaded(owner, "contract.pdf", 1000, 1, text, Instant.now());
    }

    private Contract awaitStatus(Long id, ContractStatus expected) {
        return Polling.await(() -> contracts.findById(id).orElseThrow(), c -> c.getStatus() == expected);
    }

    private static ContractAnalysisResult fourFindings() {
        return new ContractAnalysisResult("Consulting agreement", "A consulting agreement with uncapped liability.",
                List.of(
                        finding(RiskCategory.LIABILITY, 85, false, "Section 7.2",
                                "The Contractor shall be liable without limitation as to amount or type.",
                                "Limit liability to the fees paid under this Agreement (a cap)."),
                        finding(RiskCategory.IP_RIGHTS, 78, false, "Section 4",
                                "All work product, including pre-existing materials, belongs to the Client.",
                                "Exclude pre-existing materials and grant the Client a license to them."),
                        finding(RiskCategory.PAYMENT_TERMS, 55, false, "Section 3",
                                "Invoices are payable net 60.", "Invoices are payable within 30 days."),
                        finding(RiskCategory.PAYMENT_TERMS, 20, true, "Section 3.2",
                                "A late fee of 1.5% per month applies to overdue invoices.", null)));
    }

    private static ContractAnalysisResult.Finding finding(RiskCategory category, int score, boolean favorable,
                                                          String section, String quote, String revision) {
        return new ContractAnalysisResult.Finding(category, score, favorable, category + " clause", section, quote,
                "Why this matters for the freelancer.", revision);
    }
}
