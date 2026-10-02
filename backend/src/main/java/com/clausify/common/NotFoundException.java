package com.clausify.common;

/** 404 Not Found. Use the same message for a missing record and one owned by another user, so the response never reveals that it exists. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
