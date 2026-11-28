package com.clausify.clause;

import com.clausify.analysis.RiskCategory;

/** A library clause as the API shows it. Never includes the embedding. */
public record StandardClauseResponse(Long id, RiskCategory category, String title, String text, boolean favorable,
                                     String notes) {

    static StandardClauseResponse from(StandardClause clause) {
        return new StandardClauseResponse(clause.getId(), clause.getCategory(), clause.getTitle(), clause.getText(),
                clause.isFavorable(), clause.getNotes());
    }
}
