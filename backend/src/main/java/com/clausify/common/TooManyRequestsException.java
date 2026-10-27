package com.clausify.common;

/** 429 Too Many Requests: a rate limit was hit. {@code retryAfterSeconds} becomes the Retry-After header. */
public class TooManyRequestsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(long retryAfterSeconds) {
        super(message(retryAfterSeconds));
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    private static String message(long seconds) {
        if (seconds < 60) {
            return "Too many requests. Please wait a moment and try again.";
        }
        long minutes = (seconds + 59) / 60;
        return "Too many requests. Please try again in " + minutes + (minutes == 1 ? " minute." : " minutes.");
    }
}
