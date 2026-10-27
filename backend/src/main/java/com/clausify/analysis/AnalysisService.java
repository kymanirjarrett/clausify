package com.clausify.analysis;

import com.clausify.analysis.dto.ContractAnalysisResponse;
import com.clausify.analysis.dto.ContractAnalysisResponse.ExtractedFinding;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
public class AnalysisService {

    private final GroqContractAnalyzer contractAnalyzer;
    private final RiskScorer riskScorer;

    public AnalysisService(GroqContractAnalyzer contractAnalyzer, RiskScorer riskScorer) {
        this.contractAnalyzer = contractAnalyzer;
        this.riskScorer = riskScorer;
    }

    @Async("analysisTaskExecutor") // Processes background tasks without locking up main API server threads
    @Transactional
    public void processContractAnalysisAsynchronously(Long contractId, String contractText) {
        try {
            // 1. Core State Transition: Set current pipeline status metadata to PROCESSING
            // updateAnalysisStatus(contractId, "PROCESSING");

            // 2. Query Groq API via Spring AI Context Builder
            ContractAnalysisResponse response = contractAnalyzer.analyzeContractText(contractText);

            if (response == null || response.getFindings() == null) {
                throw new RuntimeException("Groq contract analysis pipeline yielded empty response payload.");
            }

            // 3. Score findings deterministically using your RiskScorer logic
            int calculatedScore = riskScorer.computeOverallScore(response.getFindings());
            String primaryRiskRating = riskScorer.resolveOverallRiskLevel(calculatedScore);

            // 4. Transform entities and persist records using your repositories
            for (ExtractedFinding rawFinding : response.getFindings()) {
                int numericSeverity = riskScorer.assignFindingScore(rawFinding.getRiskLevel());
                // Save ClauseFinding entity instances here mapped back to your V6 table columns
            }

            // 5. Final State Transition: Update baseline records status to COMPLETED
            // saveFinalAnalysisMetrics(contractId, "COMPLETED", calculatedScore, primaryRiskRating);

        } catch (Exception pipelineException) {
            // Example 4 Safe Catch: Marks pipeline state failed cleanly with readable reason
            // markAnalysisPipelineAsFailed(contractId, "FAILED", pipelineException.getMessage());
            System.err.println("Pipeline execution crashed on contract record: " + contractId + " -> " + pipelineException.getMessage());
        }
    }
}
