package com.clausify.contract;

/**
 * Published after a contract is saved (upload) or reset (retry). The analysis listener (FR-5) handles it
 * after the transaction commits, so uploading never waits for the AI.
 */
public record ContractUploadedEvent(Long contractId) {
}
