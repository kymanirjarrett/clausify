package com.clausify.common;

/** 409 Conflict: the request clashes with current state (duplicate email, retrying an analysis that did not fail). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
