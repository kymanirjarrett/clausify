package com.clausify.common;

/** 415 Unsupported Media Type: the uploaded file is not a PDF. */
public class UnsupportedFileTypeException extends RuntimeException {

    public UnsupportedFileTypeException(String message) {
        super(message);
    }
}
