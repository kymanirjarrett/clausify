package com.clausify.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Request limits from {@code clausify.rate-limit} (#80, ADR 0009). Each limit allows {@code capacity} requests
 * per {@code period}, refilling smoothly, so a burst is allowed but a steady flood is not.
 */
@ConfigurationProperties("clausify.rate-limit")
public record RateLimitProperties(boolean enabled, Limit login, Limit register, Limit loginFailuresPerEmail,
                                  Limit analysis) {

    public record Limit(int capacity, Duration period) {

        public Limit {
            if (capacity < 1 || period == null || period.isZero() || period.isNegative()) {
                throw new IllegalArgumentException("A rate limit needs a positive capacity and period.");
            }
        }
    }
}
