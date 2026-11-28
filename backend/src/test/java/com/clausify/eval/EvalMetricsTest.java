package com.clausify.eval;

import com.clausify.analysis.ContractAnalysisResult.Finding;
import com.clausify.analysis.RiskCategory;
import com.clausify.analysis.RiskLevel;
import com.clausify.eval.EvalMetrics.Ratio;
import com.clausify.eval.EvalMetrics.Scores;
import com.clausify.eval.GoldContract.GoldClause;
import com.clausify.eval.GoldContract.Label;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.clausify.analysis.RiskCategory.CONFIDENTIALITY;
import static com.clausify.analysis.RiskCategory.LIABILITY;
import static com.clausify.analysis.RiskCategory.NON_COMPETE;
import static com.clausify.analysis.RiskCategory.PAYMENT_TERMS;
import static com.clausify.analysis.RiskLevel.HIGH;
import static com.clausify.analysis.RiskLevel.LOW;
import static com.clausify.analysis.RiskLevel.MEDIUM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** The course/DESIGN.md 7.9 metrics on small hand-worked examples. */
class EvalMetricsTest {

    private static final GoldClause LIABILITY_GOLD = new GoldClause("7.2", LIABILITY, false, HIGH,
            "Contractor shall be liable for any and all damages arising from the Services.");
    private static final GoldClause PAYMENT_GOLD = new GoldClause("3.1", PAYMENT_TERMS, false, MEDIUM,
            "Client shall pay invoices within sixty (60) days of receipt.");
    private static final GoldClause NON_COMPETE_GOLD = new GoldClause("6.1", NON_COMPETE, false, HIGH,
            "Contractor shall not work for any competitor anywhere in the world for three years.");
    private static final GoldClause FAVORABLE_GOLD = new GoldClause("9.3", RiskCategory.TERMINATION, true, LOW,
            "Upon termination, Client shall pay for all Services performed through the termination date.");

    @Test
    void aQuoteCoveringHalfTheWordsOrTheSameSectionMatches() {
        Finding quoteOnly = finding(LIABILITY, 85, null, "Contractor shall be liable for any and all damages");
        Finding sectionOnly = finding(LIABILITY, 85, "Section 7.2.", "Different words entirely.");
        Finding wrongCategory = finding(RiskCategory.INDEMNIFICATION, 85, "7.2", LIABILITY_GOLD.text());
        Finding tooLittle = finding(LIABILITY, 85, null, "Contractor shall be liable.");

        assertThat(EvalMetrics.match(List.of(quoteOnly), List.of(LIABILITY_GOLD)).goldToFinding()).containsEntry(0, 0);
        assertThat(EvalMetrics.match(List.of(sectionOnly), List.of(LIABILITY_GOLD)).goldToFinding()).containsEntry(0, 0);
        assertThat(EvalMetrics.match(List.of(wrongCategory), List.of(LIABILITY_GOLD)).goldToFinding()).isEmpty();
        assertThat(EvalMetrics.match(List.of(tooLittle), List.of(LIABILITY_GOLD)).goldToFinding()).isEmpty();
    }

    @Test
    void eachFindingAndEachGoldClauseIsUsedOnce() {
        Finding twice = finding(LIABILITY, 85, "7.2", LIABILITY_GOLD.text());
        Finding again = finding(LIABILITY, 80, "7.2", LIABILITY_GOLD.text());

        assertThat(EvalMetrics.match(List.of(twice, again), List.of(LIABILITY_GOLD)).goldToFinding()).hasSize(1);
    }

    /**
     * Gold: liability HIGH, payment MEDIUM, non-compete HIGH (risky) and one favorable clause.
     * Model: liability at 85 (HIGH, exact), payment at 30 (LOW, one level off), the favorable clause, and one
     * confidentiality finding that is not in the gold set. Non-compete is missed.
     */
    @Test
    void scoresRecallPrecisionCategoriesAndLevels() {
        GoldContract contract = contract(List.of(LIABILITY_GOLD, PAYMENT_GOLD, NON_COMPETE_GOLD, FAVORABLE_GOLD));
        List<Finding> findings = List.of(
                finding(LIABILITY, 85, "7.2", LIABILITY_GOLD.text()),
                finding(PAYMENT_TERMS, 30, "Section 3.1", "within sixty (60) days"),
                new Finding(RiskCategory.TERMINATION, 10, true, "", "9.3", FAVORABLE_GOLD.text(), "", null),
                finding(CONFIDENTIALITY, 50, "8", "Confidential forever."));

        Scores scores = EvalMetrics.score(List.of(contract), List.of(findings));

        assertThat(scores.recall()).isEqualTo(new Ratio(2, 3));
        assertThat(scores.precision()).isEqualTo(new Ratio(2, 3));
        assertThat(scores.recallByCategory()).containsEntry(LIABILITY, new Ratio(1, 1))
                .containsEntry(NON_COMPETE, new Ratio(0, 1)).doesNotContainKey(RiskCategory.TERMINATION);
        assertThat(scores.levelExact()).isEqualTo(new Ratio(1, 2));
        assertThat(scores.levelWithinOne()).isEqualTo(new Ratio(2, 2));
        // Gold overall: risky 85, 55, 85 -> 0.6 * 85 + 0.4 * 75 = 81 HIGH. Model: 85, 30, 50 -> 51 + 22 = 73 HIGH.
        assertThat(scores.overallLevel()).isEqualTo(new Ratio(1, 1));
    }

    @Test
    void wilsonIntervalMatchesTheDesignDocExample() {
        // course/DESIGN.md 7.9: 85% recall over about 120 clauses gives roughly 78% to 90%.
        double[] interval = new Ratio(102, 120).wilson95();
        assertThat(interval[0]).isCloseTo(0.776, within(0.005));
        assertThat(interval[1]).isCloseTo(0.903, within(0.005));
        assertThat(new Ratio(0, 0).wilson95()[0]).isNaN();
        assertThat(new Ratio(10, 10).wilson95()[1]).isEqualTo(1.0);
    }

    @Test
    void kappaIsOneForIdenticalLabelsAndLowerWhenLabelersDisagree() {
        List<Label> a = List.of(label("7.2", LIABILITY, HIGH), label("3.1", PAYMENT_TERMS, MEDIUM),
                label("6.1", NON_COMPETE, HIGH), label("9.3", RiskCategory.TERMINATION, LOW));
        List<Label> b = List.of(label("Section 7.2", LIABILITY, HIGH), label("3.1", PAYMENT_TERMS, LOW),
                label("6.1", NON_COMPETE, HIGH));

        assertThat(EvalMetrics.cohensKappa(List.of(withLabels(a, a)))).isEqualTo(1.0);
        // 4 items: agree on 7.2 and 6.1; disagree on 3.1 (MEDIUM/LOW) and 9.3 (LOW/NONE).
        // Observed 0.5. Expected: HIGH 2/4*2/4 + MEDIUM 1/4*0 + LOW 1/4*1/4 + NONE 0*1/4 = 0.3125. Kappa = 0.1875/0.6875.
        assertThat(EvalMetrics.cohensKappa(List.of(withLabels(a, b)))).isCloseTo(0.2727, within(0.001));
    }

    @Test
    void sectionReferencesAreNormalized() {
        assertThat(EvalMetrics.sameSection("Section 7.2", "§ 7.2.")).isTrue();
        assertThat(EvalMetrics.sameSection("7.2", "7.3")).isFalse();
        assertThat(EvalMetrics.sameSection(null, null)).isFalse();
    }

    private static Finding finding(RiskCategory category, int score, String section, String quote) {
        return new Finding(category, score, false, "", section, quote, "", "");
    }

    private static Label label(String key, RiskCategory category, RiskLevel level) {
        return new Label(key, category, false, level);
    }

    private static GoldContract contract(List<GoldClause> gold) {
        return new GoldContract("t-1", "SYNTHETIC", null, List.of(), List.of(), gold, null);
    }

    private static GoldContract withLabels(List<Label> a, List<Label> b) {
        return new GoldContract("t-1", "SYNTHETIC", null, a, b, List.of(), null);
    }
}
