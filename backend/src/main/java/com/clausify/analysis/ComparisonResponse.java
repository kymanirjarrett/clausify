package com.clausify.analysis;

import com.clausify.contract.Contract;

import java.time.Instant;
import java.util.List;

/**
 * FR-10: two analyzed contracts side by side. {@code scoreChange} is revised minus original (negative means less
 * risk). {@code categoryChanges} lists only categories whose highest risky score differs; a null score means the
 * category had no risky finding in that version. {@code rows} is the text diff; when {@code unrelated} is true the
 * documents share almost no wording and the client should warn before showing it.
 */
public record ComparisonResponse(
        Version original,
        Version revised,
        int scoreChange,
        double similarity,
        boolean unrelated,
        List<CategoryChange> categoryChanges,
        Counts counts,
        List<TextDiff.Row> rows) {

    public record Version(Long id, String filename, Instant uploadedAt, int overallScore, RiskLevel riskLevel) {

        static Version of(Contract contract, Analysis analysis) {
            return new Version(contract.getId(), contract.getOriginalFilename(), contract.getUploadedAt(),
                    analysis.getOverallScore(), analysis.getRiskLevel());
        }
    }

    public record CategoryChange(RiskCategory category, Integer before, Integer after) {
    }

    /** How many stretches of text were added, removed, or edited. */
    public record Counts(long added, long removed, long changed) {
    }
}
