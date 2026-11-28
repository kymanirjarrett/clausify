package com.clausify.eval;

import com.clausify.analysis.RiskCategory;
import com.clausify.analysis.RiskLevel;

import java.util.List;

/**
 * One labeled evaluation contract (docs/evaluation/labeling-guide.md). {@code labelerA} and {@code labelerB} are
 * the independent first passes, used only for Cohen's kappa; {@code gold} is the reconciled answer key.
 */
public record GoldContract(String id, String source, String sourceRef, List<Label> labelerA, List<Label> labelerB,
                           List<GoldClause> gold, String notes) {

    /** One labeler's first-pass judgment of one clause. */
    public record Label(String key, RiskCategory category, boolean favorable, RiskLevel level) {
    }

    /** One reconciled clause in the answer key, with its exact text for matching. */
    public record GoldClause(String sectionRef, RiskCategory category, boolean favorable, RiskLevel level, String text) {
    }
}
