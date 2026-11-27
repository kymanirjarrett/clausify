package com.clausify.analysis;

import com.clausify.common.ConflictException;
import com.clausify.common.NotFoundException;
import com.clausify.contract.Contract;
import com.clausify.contract.ContractRepository;
import com.clausify.contract.ContractStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** FR-6: reads one finished analysis for its owner. Writing analyses is AnalysisService's job. */
@Service
public class AnalysisResultService {

    static final String NOT_FOUND = "Contract not found.";

    private final ContractRepository contracts;
    private final AnalysisRepository analyses;

    public AnalysisResultService(ContractRepository contracts, AnalysisRepository analyses) {
        this.contracts = contracts;
        this.analyses = analyses;
    }

    /** 404 if the contract is missing or not the caller's; 409 with the current status if it is not ANALYZED. */
    @Transactional(readOnly = true)
    public AnalysisResponse get(Long contractId, Long userId) {
        Contract contract = contracts.findByIdAndUserId(contractId, userId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
        if (contract.getStatus() != ContractStatus.ANALYZED) {
            throw new ConflictException("The analysis is not ready. The contract is " + contract.getStatus() + ".",
                    Map.of("contractStatus", contract.getStatus()));
        }
        // ANALYZED is only set in the same transaction that saves the analysis, so a missing row is a server bug.
        Analysis analysis = analyses.findByContractId(contract.getId())
                .orElseThrow(() -> new IllegalStateException("Contract " + contract.getId() + " is ANALYZED without an analysis"));
        return AnalysisResponse.from(contract, analysis);
    }
}
