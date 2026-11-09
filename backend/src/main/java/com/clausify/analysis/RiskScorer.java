package com.clausify.analysis;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Computes a contract's overall risk from its findings (DESIGN.md 7.6). The model scores each clause; the
 * overall score is computed here, in Java, so it is consistent and testable.
 *
 * <p>Only non-favorable findings count: {@code overall = round(0.6 × highest + 0.4 × average)}. Weighting the
 * highest score means one severe clause cannot be averaged away by many mild ones. With no non-favorable
 * findings the overall score is 0.
 */
@Component
public class RiskScorer {

    /** The overall score (0 to 100) and its band. */
    public record RiskScore(int overall, RiskLevel level) {
    }

    /**
     * @param findings validated findings; every score must already be within 0 to 100
     * @throws IllegalArgumentException if a score is out of range, which means validation upstream failed
     */
    public RiskScore score(List<ContractAnalysisResult.Finding> findings) {
        long count = 0;
        long sum = 0;
        int highest = 0;
        for (ContractAnalysisResult.Finding finding : findings) {
            int score = finding.riskScore();
            if (score < 0 || score > 100) {
                throw new IllegalArgumentException("Risk score out of range: " + score);
            }
            if (finding.favorable()) {
                continue;
            }
            count++;
            sum += score;
            highest = Math.max(highest, score);
        }
        if (count == 0) {
            return new RiskScore(0, RiskLevel.LOW);
        }
        // 0.6 × highest + 0.4 × (sum / count) == (3 × highest × count + 2 × sum) / (5 × count).
        // Whole numbers keep the rounding exact; doubles can put a .5 on the wrong side of a band boundary.
        long numerator = 3L * highest * count + 2L * sum;
        long denominator = 5L * count;
        int overall = (int) ((2 * numerator + denominator) / (2 * denominator)); // round half up
        return new RiskScore(overall, RiskLevel.fromScore(overall));
    }
}
