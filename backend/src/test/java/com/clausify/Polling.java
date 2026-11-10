package com.clausify;

import java.time.Duration;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Waits for background work in tests. A fixed {@code Thread.sleep} is either too short (flaky) or too long (slow);
 * polling returns as soon as the condition holds and fails with the last value seen if it never does.
 */
public final class Polling {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    private Polling() {
    }

    public static <T> T await(Supplier<T> read, Predicate<T> done) {
        return await(read, done, DEFAULT_TIMEOUT);
    }

    public static <T> T await(Supplier<T> read, Predicate<T> done, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            T value = read.get();
            if (done.test(value)) {
                return value;
            }
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Condition not met within " + timeout + "; last value: " + value);
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting", e);
            }
        }
    }
}
