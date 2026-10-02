package com.clausify.contract;

import java.time.Instant;

/**
 * One contract with its status details, polled by the Dashboard while analysis runs (FR-4).
 * {@code failureReason} is set only when status is FAILED. Never includes the extracted text.
 */
public record ContractDetailResponse(
        Long id,
        String filename,
        long sizeBytes,
        int pageCount,
        ContractStatus status,
        String failureReason,
        Instant uploadedAt,
        Instant analyzedAt) {

    static ContractDetailResponse from(Contract contract) {
        return new ContractDetailResponse(contract.getId(), contract.getOriginalFilename(), contract.getFileSizeBytes(),
                contract.getPageCount(), contract.getStatus(), contract.getFailureReason(),
                contract.getUploadedAt(), contract.getAnalyzedAt());
    }
}
