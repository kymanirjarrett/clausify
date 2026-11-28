package com.clausify.analysis;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The band edges from docs/spec.md, Risk scoring (#35). */
class RiskLevelTest {

    @ParameterizedTest
    @CsvSource({"0, LOW", "39, LOW", "40, MEDIUM", "69, MEDIUM", "70, HIGH", "100, HIGH"})
    void fromScoreUsesTheDesignBands(int score, RiskLevel expected) {
        assertThat(RiskLevel.fromScore(score)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 101, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void fromScoreRejectsScoresOutsideZeroToHundred(int score) {
        assertThatThrownBy(() -> RiskLevel.fromScore(score)).isInstanceOf(IllegalArgumentException.class);
    }
}
