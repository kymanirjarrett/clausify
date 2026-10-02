package com.clausify.common;

/** 422 Unprocessable Entity: the upload is a PDF, but no usable text could be extracted (scanned, encrypted, or corrupt). */
public class UnreadablePdfException extends RuntimeException {

    public UnreadablePdfException(String message) {
        super(message);
    }
}
