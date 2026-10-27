package com.clausify.common;

import com.clausify.config.RateLimitProperties;
import com.clausify.config.RateLimitProperties.Limit;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterTest {

    private static final Limit THREE_PER_MINUTE = new Limit(3, Duration.ofMinutes(1));

    private final MutableClock clock = new MutableClock();
    private final RateLimiter limiter = new RateLimiter(clock, properties(true));

    @Test
    void allowsUpToCapacityThenRejectsWithRetryAfter() {
        for (int i = 0; i < 3; i++) {
            limiter.acquire("k", THREE_PER_MINUTE);
        }

        assertThatThrownBy(() -> limiter.acquire("k", THREE_PER_MINUTE))
                .isInstanceOfSatisfying(TooManyRequestsException.class,
                        // One token refills every 20 seconds.
                        ex -> assertThat(ex.getRetryAfterSeconds()).isEqualTo(20));
    }

    @Test
    void refillsOverTime() {
        exhaust("k");

        clock.advance(Duration.ofSeconds(20));

        assertThatCode(() -> limiter.acquire("k", THREE_PER_MINUTE)).doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.acquire("k", THREE_PER_MINUTE)).isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void keysAreIndependent() {
        exhaust("maria");

        assertThatCode(() -> limiter.acquire("bob", THREE_PER_MINUTE)).doesNotThrowAnyException();
    }

    @Test
    void requireAvailableChecksWithoutTakingATokenAndRecordNeverThrows() {
        limiter.requireAvailable("k", THREE_PER_MINUTE);
        limiter.requireAvailable("k", THREE_PER_MINUTE);
        exhaust("k");

        assertThatCode(() -> limiter.record("k", THREE_PER_MINUTE)).doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.requireAvailable("k", THREE_PER_MINUTE))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void doesNothingWhenDisabled() {
        RateLimiter disabled = new RateLimiter(clock, properties(false));

        assertThatCode(() -> {
            for (int i = 0; i < 10; i++) {
                disabled.acquire("k", THREE_PER_MINUTE);
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void messageTellsTheUserRoughlyHowLongToWait() {
        assertThat(new TooManyRequestsException(20).getMessage()).isEqualTo("Too many requests. Please wait a moment and try again.");
        assertThat(new TooManyRequestsException(61).getMessage()).isEqualTo("Too many requests. Please try again in 2 minutes.");
    }

    private void exhaust(String key) {
        for (int i = 0; i < 3; i++) {
            limiter.acquire(key, THREE_PER_MINUTE);
        }
    }

    private static RateLimitProperties properties(boolean enabled) {
        return new RateLimitProperties(enabled, THREE_PER_MINUTE, THREE_PER_MINUTE, THREE_PER_MINUTE, THREE_PER_MINUTE);
    }

    private static final class MutableClock extends Clock {

        private Instant now = Instant.parse("2026-10-01T09:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
