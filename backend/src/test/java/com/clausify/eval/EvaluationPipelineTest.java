package com.clausify.eval;

import com.clausify.analysis.ContractAnalysisResult;
import com.clausify.analysis.ContractAnalysisResult.Finding;
import com.clausify.analysis.ContractAnalyzer;
import com.clausify.analysis.RiskCategory;
import com.clausify.eval.EvalMetrics.Ratio;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the whole evaluation pipeline in CI without Groq: load the sample split, analyze with a stub that returns the
 * local fake analyzer's findings, score, and render the report. The stub misses the sample's non-compete clause,
 * so recall is 3 of 4.
 */
class EvaluationPipelineTest {

    private static final ContractAnalyzer STUB = new ContractAnalyzer() {
        @Override
        public ContractAnalysisResult analyze(String contractText) {
            return new ContractAnalysisResult("Consulting Agreement", null, List.of(
                    new Finding(RiskCategory.LIABILITY, 85, false, "", "Section 7.2",
                            "Contractor shall be liable for any and all damages arising from the Services.", "", ""),
                    new Finding(RiskCategory.IP_RIGHTS, 78, false, "", "Section 5.1",
                            "All work product is the sole property of Client upon creation.", "", ""),
                    new Finding(RiskCategory.PAYMENT_TERMS, 55, false, "", "Section 3.2",
                            "Client shall pay invoices within sixty (60) days of receipt.", "", ""),
                    new Finding(RiskCategory.TERMINATION, 20, true, "", "Section 9.3",
                            "Upon termination, Client shall pay for all Services performed through the termination date.", "", null)));
        }

        @Override
        public String modelName() {
            return "stub";
        }
    };

    @Test
    void loadsScoresAndReportsTheSampleSplit() throws Exception {
        List<EvaluationRunner.Case> cases = EvaluationRunner.load("sample");
        assertThat(cases).singleElement().satisfies(c -> {
            assertThat(c.gold().id()).isEqualTo("sample-01");
            assertThat(c.text()).contains("7.2 Liability.");
        });

        List<EvaluationRunner.Run> runs = EvaluationRunner.run(STUB, cases, 2);

        assertThat(runs).hasSize(2).allSatisfy(run -> {
            assertThat(run.failures()).isZero();
            assertThat(run.scores().recall()).isEqualTo(new Ratio(3, 4));
            assertThat(run.scores().precision()).isEqualTo(new Ratio(3, 3));
            assertThat(run.scores().levelExact()).isEqualTo(new Ratio(3, 3));
            assertThat(run.scores().recallByCategory()).containsEntry(RiskCategory.NON_COMPETE, new Ratio(0, 1));
        });

        String report = EvaluationRunner.report("sample", "stub", EvaluationRunner.promptVersion(), cases, runs);
        assertThat(report)
                .contains("| Model | `stub` |", "| Gold risky clauses | 4 |", "| Runs | 2 |")
                .contains("| Recall | 75.0% (3/4) | 75.0% (3/4) | 75.0% |")
                .contains("| NON_COMPETE | 0.0% (0/1)")
                .doesNotContain("n/a (no first-pass labels)");
    }

    @Test
    void aFailedAnalysisCountsAsMissedClauses() throws Exception {
        ContractAnalyzer failing = new ContractAnalyzer() {
            @Override
            public ContractAnalysisResult analyze(String contractText) {
                throw new IllegalStateException("Groq is down");
            }

            @Override
            public String modelName() {
                return "failing";
            }
        };

        List<EvaluationRunner.Run> runs = EvaluationRunner.run(failing, EvaluationRunner.load("sample"), 1);

        assertThat(runs.getFirst().failures()).isEqualTo(1);
        assertThat(runs.getFirst().scores().recall()).isEqualTo(new Ratio(0, 4));
    }
}
