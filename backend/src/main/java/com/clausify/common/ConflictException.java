package com.clausify.common;

import java.util.Map;

/** 409 Conflict: the request clashes with current state (duplicate email, retrying an analysis that did not fail). */
public class ConflictException extends RuntimeException {

    /** Extra ProblemDetail properties, e.g. the contract's current status when its analysis is not ready. */
    private final Map<String, Object> properties;

    public ConflictException(String message) {
        this(message, Map.of());
    }

    public ConflictException(String message, Map<String, Object> properties) {
        super(message);
        this.properties = Map.copyOf(properties);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
