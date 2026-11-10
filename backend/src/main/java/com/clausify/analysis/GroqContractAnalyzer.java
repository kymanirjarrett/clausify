package com.clausify.analysis;

import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIServiceException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * The only class that calls an LLM (CLAUDE.md, "AI integration"; ADR 0006). Active only in {@code prod}.
 *
 * <p>How one analysis runs:
 * <ol>
 *   <li>Split the contract into chunks that fit Groq's per-minute token limit ({@link ContractChunker}).</li>
 *   <li>Send each chunk, one at a time, inside {@code <contract_text>} delimiters, asking for JSON that Groq's strict
 *       schema mode enforces ({@code prompts/contract-analysis.schema.json}).</li>
 *   <li>Validate every reply ({@link ModelOutputParser}). An invalid reply is retried once; a second invalid reply
 *       fails the whole analysis, so a contract is never saved with some of its chunks missing.</li>
 *   <li>Merge the chunks' findings, dropping duplicates.</li>
 * </ol>
 *
 * <p>Rate limits: the OpenAI SDK's own retries are turned off ({@code spring.ai.openai.max-retries: 0}) because it caps
 * every wait at 8 seconds, while Groq's per-minute limit can ask for up to a minute. Here a 429 waits for Groq's
 * {@code Retry-After}, up to {@code max-rate-limit-wait}. A longer wait means the daily limit is used up, so the
 * analysis fails at once with a clear message instead of holding a worker for hours.
 */
@Component
@Profile("prod")
public class GroqContractAnalyzer implements ContractAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(GroqContractAnalyzer.class);

    /** Removes any copy of our delimiter from the contract, so the text cannot close the data block early. */
    private static final Pattern DELIMITER = Pattern.compile("(?i)<\\s*/?\\s*contract_text\\s*>");
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(30);
    /** A typical page of contract text, used only to state the length limit in words people understand. */
    private static final int CHARS_PER_PAGE = 3000;

    static final String INVALID_REPLY = "The AI returned a response we couldn't use. Please retry the analysis.";
    static final String USAGE_LIMIT = "The AI service's usage limit has been reached. Please retry later.";
    static final String BUSY = "The AI service is busy. Please retry in a few minutes.";
    static final String UNAVAILABLE = "The AI service is unavailable right now. Please retry later.";

    /** Waits between attempts; replaced in tests so they don't sleep. */
    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private final OpenAiChatModel chatModel;
    private final GroqAnalyzerProperties properties;
    private final Sleeper sleeper;
    private final String systemPrompt;
    private final String userTemplate;
    private final OpenAiChatOptions options;

    @Autowired
    public GroqContractAnalyzer(OpenAiChatModel chatModel, GroqAnalyzerProperties properties) {
        this(chatModel, properties, Thread::sleep);
    }

    GroqContractAnalyzer(OpenAiChatModel chatModel, GroqAnalyzerProperties properties, Sleeper sleeper) {
        this.chatModel = chatModel;
        this.properties = properties;
        this.sleeper = sleeper;
        this.systemPrompt = resource("prompts/contract-analysis-system.md")
                .replace("{{MAX_FINDINGS}}", Integer.toString(properties.maxFindingsPerChunk()));
        this.userTemplate = resource("prompts/contract-analysis-user.md");
        // Start from the configured options (model, reasoning replay) and change only what analysis needs. Building
        // fresh options instead would silently send Spring AI's built-in default model (GroqContractAnalyzerStubTest).
        this.options = chatModel.getOptions().mutate()
                .temperature(properties.temperature())
                .reasoningEffort(properties.reasoningEffort())
                .maxCompletionTokens(properties.maxCompletionTokens())
                .responseFormat(OpenAiChatModel.ResponseFormat.builder()
                        .type(OpenAiChatModel.ResponseFormat.Type.JSON_SCHEMA)
                        .jsonSchema(resource("prompts/contract-analysis.schema.json"))
                        .strict(true)
                        .build())
                .build();
    }

    @Override
    public String modelName() {
        return chatModel.getOptions().getModel();
    }

    @Override
    public ContractAnalysisResult analyze(String contractText) {
        List<String> chunks = ContractChunker.split(contractText == null ? "" : contractText,
                properties.maxChunkChars());
        if (chunks.isEmpty()) {
            throw new ContractAnalysisException("The contract has no text to analyze.");
        }
        if (chunks.size() > properties.maxChunks()) {
            int pages = properties.maxChunks() * properties.maxChunkChars() / CHARS_PER_PAGE;
            throw new ContractAnalysisException(
                    "This contract is too long to analyze. The limit is about " + pages + " pages.");
        }

        String contractType = null;
        String summary = null;
        Map<String, ContractAnalysisResult.Finding> findings = new LinkedHashMap<>();
        for (int i = 0; i < chunks.size(); i++) {
            ContractAnalysisResult part = analyzeChunk(chunks.get(i), i + 1, chunks.size());
            contractType = contractType != null ? contractType : part.contractType();
            summary = summary != null ? summary : part.summary();
            for (ContractAnalysisResult.Finding finding : part.findings()) {
                // The same clause can be reported twice; keep the higher score.
                findings.merge(dedupKey(finding), finding,
                        (a, b) -> b.riskScore() > a.riskScore() ? b : a);
            }
        }
        return new ContractAnalysisResult(contractType, summary, List.copyOf(findings.values()));
    }

    private ContractAnalysisResult analyzeChunk(String chunk, int part, int total) {
        String user = userTemplate
                .replace("{{PART}}", Integer.toString(part))
                .replace("{{TOTAL}}", Integer.toString(total))
                .replace("{{CONTRACT_TEXT}}", DELIMITER.matcher(chunk).replaceAll(""));
        Prompt prompt = new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(user)), options);

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return ModelOutputParser.parse(call(prompt, part, total), properties.maxFindingsPerChunk());
            } catch (InvalidModelOutputException e) {
                log.warn("Invalid model reply for part {} of {} (try {}): {}", part, total, attempt, e.getMessage());
            }
        }
        throw new ContractAnalysisException(INVALID_REPLY);
    }

    private String call(Prompt prompt, int part, int total) throws InvalidModelOutputException {
        for (int attempt = 1; ; attempt++) {
            try {
                ChatResponse response = chatModel.call(prompt);
                Generation generation = response == null ? null : response.getResult();
                if (generation == null || generation.getOutput() == null) {
                    throw new InvalidModelOutputException("reply had no message");
                }
                String finish = generation.getMetadata().getFinishReason();
                if (finish != null && finish.toLowerCase(Locale.ROOT).contains("length")) {
                    throw new InvalidModelOutputException("reply was cut off at the token limit");
                }
                Usage usage = response.getMetadata().getUsage();
                log.info("Analyzed part {} of {}: {} prompt + {} completion tokens", part, total,
                        usage == null ? null : usage.getPromptTokens(),
                        usage == null ? null : usage.getCompletionTokens());
                return generation.getOutput().getText();
            } catch (RuntimeException e) {
                waitBeforeRetry(e, attempt);
            }
        }
    }

    /** Waits before the next attempt, or throws if this failure should not or cannot be retried. */
    private void waitBeforeRetry(RuntimeException e, int attempt) {
        OpenAIServiceException service = cause(e, OpenAIServiceException.class);
        Duration wait;
        if (service != null && service.statusCode() == 429) {
            wait = retryAfter(service).orElse(backoff(attempt));
            if (wait.compareTo(properties.maxRateLimitWait()) > 0) {
                log.warn("Groq asked to wait {}s, longer than allowed; treating it as the daily limit",
                        wait.toSeconds());
                throw new ContractAnalysisException(USAGE_LIMIT, e);
            }
            if (attempt >= properties.maxAttempts()) {
                throw new ContractAnalysisException(BUSY, e);
            }
        } else if ((service != null && service.statusCode() >= 500) || cause(e, OpenAIIoException.class) != null) {
            if (attempt >= properties.maxAttempts()) {
                throw new ContractAnalysisException(UNAVAILABLE, e);
            }
            wait = backoff(attempt);
        } else {
            // 400, 401, 403, 404 and anything unexpected: retrying cannot help.
            log.error("Groq request failed with status {}", service == null ? "none" : service.statusCode());
            throw new ContractAnalysisException("The AI analysis failed. Please retry later.", e);
        }
        log.info("Groq attempt {} failed; retrying in {} ms", attempt, wait.toMillis());
        try {
            sleeper.sleep(wait);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ContractAnalysisException("The analysis was interrupted.", interrupted);
        }
    }

    private static Optional<Duration> retryAfter(OpenAIServiceException e) {
        return e.headers().values("retry-after").stream().findFirst().flatMap(value -> {
            try {
                return Optional.of(Duration.ofMillis(Math.round(Double.parseDouble(value.strip()) * 1000)));
            } catch (NumberFormatException notSeconds) {
                return Optional.empty(); // an HTTP date; fall back to our own backoff
            }
        });
    }

    private static Duration backoff(int attempt) {
        Duration wait = Duration.ofSeconds(1L << Math.min(attempt, 5)); // 2, 4, 8, 16, 32 seconds
        return wait.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : wait;
    }

    private static <T extends Throwable> T cause(Throwable e, Class<T> type) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return type.cast(t);
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return null;
    }

    private static String dedupKey(ContractAnalysisResult.Finding finding) {
        return finding.category() + "|" + finding.originalText().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static String resource(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + path, e);
        }
    }
}
