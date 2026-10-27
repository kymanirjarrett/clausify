package com.clausify.analysis;

public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH;

    public static RiskLevel fromScore(int score) {
        if (score < 40) return LOW;
        if (score < 75) return MEDIUM;
        return HIGH;
    }
}
