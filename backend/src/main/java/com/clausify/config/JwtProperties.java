package com.clausify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * JWT settings from {@code clausify.jwt.*}. The secret signs every token (HS256), so anyone who knows it
 * can forge logins: it comes from {@code JWT_SECRET} in production and must be at least 32 bytes
 * (256 bits, the HS256 key size). A shorter secret stops the app at startup instead of running insecurely.
 */
@ConfigurationProperties("clausify.jwt")
public record JwtProperties(String secret, Duration expiry) {

    static final int MIN_SECRET_BYTES = 32;
    static final Duration DEFAULT_EXPIRY = Duration.ofHours(24);

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("clausify.jwt.secret (JWT_SECRET) must be at least "
                    + MIN_SECRET_BYTES + " bytes. Generate one with: openssl rand -base64 48");
        }
        expiry = expiry == null ? DEFAULT_EXPIRY : expiry;
    }

    @Override
    public String toString() {
        return "JwtProperties[secret=****, expiry=" + expiry + "]";
    }
}
