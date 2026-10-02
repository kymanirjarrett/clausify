package com.clausify.contract;

import java.time.Instant;

/** A contract in API responses. Never includes the extracted text. */
public record ContractSummaryResponse(
        Long id,
        String filename,
        long sizeBytes,
        int pageCount,
        ContractStatus status,
        Instant uploadedAt) {

    static ContractSummaryResponse from(Contract contract) {
        return new ContractSummaryResponse(contract.getId(), contract.getOriginalFilename(), contract.getFileSizeBytes(),
                contract.getPageCount(), contract.getStatus(), contract.getUploadedAt());
    }
}
