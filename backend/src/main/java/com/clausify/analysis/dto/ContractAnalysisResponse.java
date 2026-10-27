package com.clausify.analysis.dto;

import java.util.List;

public class ContractAnalysisResponse {
    private List<ExtractedFinding> findings;

    public List<ExtractedFinding> getFindings() { return findings; }
    public void setFindings(List<ExtractedFinding> findings) { this.findings = findings; }

    public static class ExtractedFinding {
        private String category;      // e.g., "Liability", "Payment"
        private String riskLevel;     // e.g., "High", "Low", "Favorable"
        private String originalText;  // Verbatim clause text from contract
        private String explanation;   // Human readable risk description
        private String suggestedRevision; // Safer contract clause alternative

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public String getOriginalText() { return originalText; }
        public void setOriginalText(String originalText) { this.originalText = originalText; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
        public String getSuggestedRevision() { return suggestedRevision; }
        public void setSuggestedRevision(String suggestedRevision) { this.suggestedRevision = suggestedRevision; }
    }
}

