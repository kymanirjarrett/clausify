package com.clausify.clause;

import com.clausify.analysis.RiskCategory;

import java.util.List;

/**
 * FR-9: the user's clause and the most similar standard clauses in the same category, most similar first.
 * {@code similarity} is cosine similarity from 0 to 1.
 */
public record SimilarClausesResponse(Finding finding, List<Match> matches) {

    public record Finding(Long id, RiskCategory category, String title, String originalText, String suggestedRevision) {
    }

    public record Match(StandardClauseResponse clause, double similarity) {
    }
}
