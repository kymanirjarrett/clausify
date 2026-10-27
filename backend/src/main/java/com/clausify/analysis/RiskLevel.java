package com.clausify.analysis;

/** Risk bands from DESIGN.md 7.6: Low below 40, Medium 40 to 69, High 70 and above. */
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH;

    public static RiskLevel fromScore(int score) {
        if (score < 40) return LOW;
        if (score < 70) return MEDIUM;
        return HIGH;
    }
}
