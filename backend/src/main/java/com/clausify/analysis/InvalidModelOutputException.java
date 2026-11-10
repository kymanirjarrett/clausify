package com.clausify.analysis;

/**
 * The model's reply broke the response contract. The message names the rule that failed (for logs) and never
 * includes the model's output or the contract text.
 */
class InvalidModelOutputException extends Exception {

    InvalidModelOutputException(String reason) {
        super(reason);
    }
}
