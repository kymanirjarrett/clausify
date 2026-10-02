package com.clausify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Browser origins allowed to call the API, from {@code clausify.cors.allowed-origins}
 * ({@code CORS_ALLOWED_ORIGINS}, comma-separated). Local default: the Angular dev server.
 */
@ConfigurationProperties("clausify.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
