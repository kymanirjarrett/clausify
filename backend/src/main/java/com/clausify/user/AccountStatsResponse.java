package com.clausify.user;

/**
 * FR-12: totals for the Account page. {@code averageRiskScore} is the mean overall score of the user's analyzed
 * contracts, rounded to a whole number, or null before the first analysis.
 */
public record AccountStatsResponse(long contractsUploaded, long contractsAnalyzed, Integer averageRiskScore) {
}
