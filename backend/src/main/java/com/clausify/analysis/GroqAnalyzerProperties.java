package com.clausify.analysis;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limits for {@link GroqContractAnalyzer}, from {@code clausify.analysis.groq}. They follow Groq's free tier for
 * {@code openai/gpt-oss-120b}: 8,000 tokens per minute and 200,000 per day. Each request carries the prompt, one
 * chunk of contract text, the model's reasoning, and its answer, so all of it must fit in one minute's budget.
 *
 * @param maxChunkChars       contract characters per request (about 4 characters per token)
 * @param maxChunks           longer contracts are rejected instead of using up the daily token budget
 * @param maxFindingsPerChunk the prompt asks for at most this many; more is treated as an invalid response
 * @param maxCompletionTokens cap on reasoning plus answer tokens per request
 * @param temperature         low for consistent scoring; null leaves the provider default
 * @param reasoningEffort     gpt-oss reasoning effort ({@code low}, {@code medium}, {@code high}); low saves tokens
 * @param maxRateLimitWait    the longest {@code Retry-After} we will wait out; a longer one means the daily limit
 * @param maxAttempts         attempts per chunk for rate limits and transient errors
 */
@ConfigurationProperties("clausify.analysis.groq")
public record GroqAnalyzerProperties(
        int maxChunkChars,
        int maxChunks,
        int maxFindingsPerChunk,
        int maxCompletionTokens,
        Double temperature,
        String reasoningEffort,
        Duration maxRateLimitWait,
        int maxAttempts) {

    public GroqAnalyzerProperties {
        if (maxChunkChars < 1000 || maxChunks < 1 || maxFindingsPerChunk < 1 || maxCompletionTokens < 256
                || maxAttempts < 1 || maxRateLimitWait == null || maxRateLimitWait.isNegative()) {
            throw new IllegalArgumentException("Invalid clausify.analysis.groq settings.");
        }
    }
}
