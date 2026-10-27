package com.clausify.common;

import com.clausify.config.RateLimitProperties;
import com.clausify.config.RateLimitProperties.Limit;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory token buckets, one per key (for example {@code "login-ip:203.0.113.7"}). A bucket holds up to
 * {@code capacity} tokens and refills continuously at {@code capacity / period}; each request takes one.
 * <p>
 * Buckets live in this instance's memory, which is correct while the API runs as a single Render instance.
 * Running several instances would need a shared store such as Redis (ADR 0009).
 */
@Component
public class RateLimiter {

    /** Sweep refilled buckets every this many calls, so memory stays bounded by recent activity. */
    private static final int SWEEP_EVERY = 1024;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicInteger calls = new AtomicInteger();
    private final Clock clock;
    private final boolean enabled;

    public RateLimiter(Clock clock, RateLimitProperties properties) {
        this.clock = clock;
        this.enabled = properties.enabled();
    }

    /** Takes one token, or throws 429 if the bucket is empty. */
    public void acquire(String key, Limit limit) {
        take(key, limit, true);
    }

    /** Throws 429 if the bucket is empty, without taking a token. */
    public void requireAvailable(String key, Limit limit) {
        if (!enabled) {
            return;
        }
        long now = clock.millis();
        Bucket bucket = buckets.get(key);
        if (bucket != null) {
            synchronized (bucket) {
                bucket.refill(now);
                if (bucket.tokens < 1) {
                    throw new TooManyRequestsException(bucket.secondsUntilToken());
                }
            }
        }
    }

    /** Takes one token if there is one and never throws: for counting failures after they happen. */
    public void record(String key, Limit limit) {
        take(key, limit, false);
    }

    private void take(String key, Limit limit, boolean throwWhenEmpty) {
        if (!enabled) {
            return;
        }
        sweepOccasionally();
        long now = clock.millis();
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(limit, now));
        synchronized (bucket) {
            bucket.refill(now);
            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
            } else if (throwWhenEmpty) {
                throw new TooManyRequestsException(bucket.secondsUntilToken());
            }
        }
    }

    private void sweepOccasionally() {
        if (calls.incrementAndGet() % SWEEP_EVERY != 0) {
            return;
        }
        long now = clock.millis();
        buckets.values().removeIf(bucket -> {
            synchronized (bucket) {
                bucket.refill(now);
                return bucket.tokens >= bucket.capacity;
            }
        });
    }

    private static final class Bucket {

        private final double capacity;
        private final double tokensPerMilli;
        private double tokens;
        private long updatedAt;

        private Bucket(Limit limit, long now) {
            this.capacity = limit.capacity();
            this.tokensPerMilli = limit.capacity() / (double) limit.period().toMillis();
            this.tokens = capacity;
            this.updatedAt = now;
        }

        private void refill(long now) {
            tokens = Math.min(capacity, tokens + (now - updatedAt) * tokensPerMilli);
            updatedAt = now;
        }

        private long secondsUntilToken() {
            return Math.max(1, (long) Math.ceil((1 - tokens) / tokensPerMilli / 1000));
        }
    }
}
