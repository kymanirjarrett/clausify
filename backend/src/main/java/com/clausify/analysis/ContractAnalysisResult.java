package com.clausify.analysis;

import java.util.List;

/**
 * What a {@link ContractAnalyzer} returns: the JSON contract in DESIGN.md 7.7, after validation.
 *
 * @param contractType a short label such as "Consulting Agreement", or null if unclear
 * @param summary one or two sentences on where the risk is concentrated, or null
 * @param findings flagged clauses, risky and favorable
 */
public record ContractAnalysisResult(String contractType, String summary, List<Finding> findings) {

    public ContractAnalysisResult {
        findings = findings == null ? List.of() : List.copyOf(findings);
    }

    /**
     * One flagged clause.
     *
     * @param riskScore 0 to 100; for a favorable finding, how strongly it favors the freelancer is not scored,
     *                  so this is the clause's residual risk (usually low)
     * @param favorable true when the clause works in the freelancer's favor ("What's in your favor")
     */
    public record Finding(
            RiskCategory category,
            int riskScore,
            boolean favorable,
            String title,
            String sectionRef,
            String originalText,
            String explanation,
            String suggestedRevision) {
    }
}
