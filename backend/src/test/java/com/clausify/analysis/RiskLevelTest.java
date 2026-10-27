package com.clausify.analysis;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** The band edges from DESIGN.md 7.6 (#35). */
class RiskLevelTest {

    @ParameterizedTest
    @CsvSource({"0, LOW", "39, LOW", "40, MEDIUM", "69, MEDIUM", "70, HIGH", "100, HIGH"})
    void fromScoreUsesTheDesignBands(int score, RiskLevel expected) {
        assertThat(RiskLevel.fromScore(score)).isEqualTo(expected);
    }
}
