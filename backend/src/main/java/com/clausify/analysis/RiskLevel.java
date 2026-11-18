package com.clausify.analysis;

/** Risk bands from DESIGN.md 7.6: Low below 40, Medium 40 to 69, High 70 and above. */
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH;

    private static final int MEDIUM_THRESHOLD = 40;
    private static final int HIGH_THRESHOLD = 70;

    public static RiskLevel fromScore(int score) {
    if (score < 0 || score > 100) {
        throw new IllegalArgumentException("Risk score must be between 0 and 100.");
    }

    if (score < MEDIUM_THRESHOLD) return LOW;
    if (score < HIGH_THRESHOLD) return MEDIUM;
    return HIGH;
}
}
