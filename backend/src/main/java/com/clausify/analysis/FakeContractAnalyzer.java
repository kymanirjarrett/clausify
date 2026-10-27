package com.clausify.analysis;

import com.clausify.analysis.dto.ContractAnalysisResponse;
import com.clausify.analysis.dto.ContractAnalysisResponse.ExtractedFinding;
import java.util.List;

public class FakeContractAnalyzer extends GroqContractAnalyzer {

    public FakeContractAnalyzer() {
        super(null); // Bypasses Spring AI ChatClient construction dependencies inside tests
    }

    @Override
    public ContractAnalysisResponse analyzeContractText(String contractText) {
        ContractAnalysisResponse mockResponse = new ContractAnalysisResponse();

        ExtractedFinding testFinding = new ExtractedFinding();
        testFinding.setCategory("Liability");
        testFinding.setRiskLevel("HIGH");
        testFinding.setOriginalText("liable without limitation as to amount or type");
        testFinding.setExplanation("This clause opens the contractor up to infinite liability.");
        testFinding.setSuggestedRevision("Liability shall be capped at total fees paid under this Agreement.");

        mockResponse.setFindings(List.of(testFinding));
        return mockResponse;
    }
}
