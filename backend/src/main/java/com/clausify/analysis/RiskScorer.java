package com.clausify.analysis;

import com.clausify.analysis.dto.ContractAnalysisResponse.ExtractedFinding;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class RiskScorer {

    // Maps string severities into concrete scores to feed the deterministic calculation engine
    public int assignFindingScore(String riskLevel) {
        if (riskLevel == null) return 0;
        return switch (riskLevel.toUpperCase()) {
            case "HIGH" -> 85;
            case "MEDIUM" -> 55;
            case "LOW" -> 20;
            case "FAVORABLE" -> 0;
            default -> 0;
        };
    }

    public int computeOverallScore(List<ExtractedFinding> findings) {
        if (findings == null || findings.isEmpty()) {
            return 0;
        }

        // Calculates deterministic mathematical mean weighted average
        double totalRiskPoints = 0;
        for (ExtractedFinding finding : findings) {
            totalRiskPoints += assignFindingScore(finding.getRiskLevel());
        }

        return (int) Math.round(totalRiskPoints / findings.size());
    }

    public String resolveOverallRiskLevel(int overallScore) {
        if (overallScore < 40) return "LOW";
        if (overallScore <= 69) return "MEDIUM";
        return "HIGH"; // 70 and above matches HIGH status tier automatically
    }
}
