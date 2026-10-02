package com.clausify.contract;

/** Analysis lifecycle: UPLOADED → ANALYZING → ANALYZED, or FAILED (retryable back to UPLOADED). */
public enum ContractStatus {
    UPLOADED,
    ANALYZING,
    ANALYZED,
    FAILED
}
