package com.clausify.clause;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The embedder for clause similarity. ADR 0012: the word-based {@link HashingEmbedder} everywhere, because a neural
 * model did not fit Render's 512 MB. A model can replace it here without touching the rest of FR-9.
 */
@Configuration(proxyBeanMethods = false)
public class EmbeddingConfig {

    @Bean
    ClauseEmbedder clauseEmbedder() {
        return new HashingEmbedder();
    }
}
