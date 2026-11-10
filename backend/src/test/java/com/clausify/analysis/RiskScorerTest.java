package com.clausify.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Expected values are worked out by hand from DESIGN.md 7.6, never computed with the scorer itself, so a wrong
 * formula cannot also produce a matching expectation.
 */
class RiskScorerTest {

    private final RiskScorer scorer = new RiskScorer();

    @Test
    void designDocWorkedExampleIs80High() {
        // 0.6 × 85 + 0.4 × (85 + 78 + 55) / 3 = 51 + 29.07 = 80.07 → 80. The favorable 20 is ignored.
        var result = scorer.score(List.of(risky(85), risky(78), risky(55), favorable(20)));

        assertThat(result.overall()).isEqualTo(80);
        assertThat(result.level()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void noFindingsScoresZero() {
        assertThat(scorer.score(List.of())).isEqualTo(new RiskScorer.RiskScore(0, RiskLevel.LOW));
    }

    @Test
    void onlyFavorableFindingsScoresZero() {
        // A favorable clause with a high number must not raise the score.
        assertThat(scorer.score(List.of(favorable(90), favorable(10))))
                .isEqualTo(new RiskScorer.RiskScore(0, RiskLevel.LOW));
    }

    @Test
    void oneSevereClauseIsNotAveragedAway() {
        // 0.6 × 95 + 0.4 × (95 + 4 × 10) / 5 = 57 + 10.8 = 67.8 → 68; a plain average would give 27.
        var findings = new ArrayList<ContractAnalysisResult.Finding>();
        findings.add(risky(95));
        for (int i = 0; i < 4; i++) findings.add(risky(10));

        assertThat(scorer.score(findings).overall()).isEqualTo(68);
    }

    @ParameterizedTest(name = "single finding {0} → {1}")
    @CsvSource({"0, LOW", "39, LOW", "40, MEDIUM", "69, MEDIUM", "70, HIGH", "100, HIGH"})
    void singleFindingScoresItselfAtEachBandBoundary(int score, RiskLevel level) {
        var result = scorer.score(List.of(risky(score)));

        assertThat(result.overall()).isEqualTo(score);
        assertThat(result.level()).isEqualTo(level);
    }

    @ParameterizedTest(name = "{0} and {1} → {2}")
    @CsvSource({
            "70, 69, 70",  // 42 + 27.8 = 69.8 → 70: rounds up into High
            "40, 37, 39",  // 24 + 15.4 = 39.4 → 39: stays Low
            "50, 0, 40",   // 30 + 10 = 40: a 0 still counts toward the average
    })
    void roundsToTheNearestWholeNumber(int first, int second, int expected) {
        assertThat(scorer.score(List.of(risky(first), risky(second))).overall()).isEqualTo(expected);
    }

    @Test
    void orderDoesNotMatter() {
        assertThat(scorer.score(List.of(risky(55), favorable(20), risky(85), risky(78))).overall()).isEqualTo(80);
    }

    @ParameterizedTest
    @CsvSource({"-1", "101"})
    void rejectsOutOfRangeScoresInsteadOfClamping(int score) {
        assertThatThrownBy(() -> scorer.score(List.of(risky(50), risky(score))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ContractAnalysisResult.Finding risky(int score) {
        return finding(score, false);
    }

    private static ContractAnalysisResult.Finding favorable(int score) {
        return finding(score, true);
    }

    private static ContractAnalysisResult.Finding finding(int score, boolean favorable) {
        return new ContractAnalysisResult.Finding(
                RiskCategory.LIABILITY, score, favorable, "Title", null, "Clause text.", "Explanation.", null);
    }
}
