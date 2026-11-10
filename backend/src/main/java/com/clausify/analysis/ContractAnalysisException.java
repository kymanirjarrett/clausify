package com.clausify.analysis;

/**
 * The analyzer could not produce a valid result (model unreachable, rate limited past the retry budget, or an
 * invalid response). The message is short and safe to show the user as the contract's failure reason; it never
 * contains model output or contract text.
 */
public class ContractAnalysisException extends RuntimeException {

    public ContractAnalysisException(String message) {
        super(message);
    }

    public ContractAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
