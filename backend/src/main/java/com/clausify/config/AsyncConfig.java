package com.clausify.config;

import com.clausify.analysis.AnalysisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * The thread pool that runs contract analysis in the background (FR-5).
 *
 * <p>It is bounded on purpose: a fixed number of threads and a fixed-size queue. An unbounded queue would accept
 * every upload and then take hours to work through them, while each queued contract's text sits in memory on a
 * 512 MB server. When the queue is full, {@code execute} throws and {@code AnalysisService} marks that contract
 * FAILED with a "busy" reason the user can act on.
 */
@Configuration(proxyBeanMethods = false)
public class AsyncConfig {

    public static final String ANALYSIS_EXECUTOR = "analysisExecutor";

    @Bean(ANALYSIS_EXECUTOR)
    ThreadPoolTaskExecutor analysisExecutor(AnalysisProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.workerThreads());
        executor.setMaxPoolSize(properties.workerThreads());
        executor.setQueueCapacity(properties.queueCapacity());
        executor.setThreadNamePrefix("analysis-");
        // On shutdown, don't hold the deploy up for a minutes-long analysis. Work cut off here is failed on the
        // next startup (AnalysisService.onApplicationReady), so the user sees a retryable reason.
        executor.setWaitForTasksToCompleteOnShutdown(false);
        return executor;
    }
}
