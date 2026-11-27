package com.clausify.analysis;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How background analysis runs ({@link AnalysisService}), from {@code clausify.analysis}. The Groq-specific limits
 * live separately in {@link GroqAnalyzerProperties} ({@code clausify.analysis.groq}).
 *
 * @param workerThreads    analyses that run at the same time. One is enough: Groq's free tier allows about one
 *                         request a minute, so a second worker would mostly wait on rate limits.
 * @param queueCapacity    analyses waiting for a worker. When the queue is full, a new upload fails at once with a
 *                         "busy" reason instead of piling up work the API cannot finish.
 * @param timeout          the longest one analysis may run, rate-limit waits included. A contract can never stay in
 *                         ANALYZING forever.
 * @param recoverOnStartup on startup, fail analyses a restart interrupted and queue uploads that never started
 */
@ConfigurationProperties("clausify.analysis")
public record AnalysisProperties(
        @DefaultValue("1") int workerThreads,
        @DefaultValue("20") int queueCapacity,
        @DefaultValue("15m") Duration timeout,
        @DefaultValue("true") boolean recoverOnStartup) {

    public AnalysisProperties {
        if (workerThreads < 1 || queueCapacity < 0 || timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Invalid clausify.analysis settings.");
        }
    }
}
