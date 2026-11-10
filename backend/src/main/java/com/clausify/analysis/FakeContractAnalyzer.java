package com.clausify.analysis;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Stand-in analyzer for local development and every test: returns the same findings for any contract, makes no
 * network calls, and needs no API key (CLAUDE.md, "AI integration"). It lives in {@code src/main} because local
 * runs use it. It deliberately has no Spring AI types.
 *
 * <p>The scores match the worked example in DESIGN.md 7.6: risky findings of 85, 78, and 55 plus one favorable
 * finding of 20, which gives an overall score of 80 (High).
 */
@Component
@Profile("!prod")
public class FakeContractAnalyzer implements ContractAnalyzer {

    static final String MODEL_NAME = "fake-analyzer";

    private static final ContractAnalysisResult RESULT = new ContractAnalysisResult(
            "Consulting Agreement",
            "Sample analysis from the local fake analyzer; no AI model was called.",
            List.of(
                    new ContractAnalysisResult.Finding(
                            RiskCategory.LIABILITY, 85, false,
                            "Unlimited liability",
                            "Section 7.2",
                            "Contractor shall be liable for any and all damages arising from the Services.",
                            "There is no cap on what you could owe if something goes wrong.",
                            "Contractor's total liability shall not exceed the fees paid in the preceding 3 months."),
                    new ContractAnalysisResult.Finding(
                            RiskCategory.IP_RIGHTS, 78, false,
                            "All IP assigned before payment",
                            "Section 5.1",
                            "All work product is the sole property of Client upon creation.",
                            "You lose ownership of your work even if the client never pays.",
                            "Ownership of the work product transfers to Client upon receipt of full payment."),
                    new ContractAnalysisResult.Finding(
                            RiskCategory.PAYMENT_TERMS, 55, false,
                            "Net 60 payment terms",
                            "Section 3.2",
                            "Client shall pay invoices within sixty (60) days of receipt.",
                            "Waiting two months to be paid strains your cash flow.",
                            "Client shall pay invoices within fifteen (15) days of receipt."),
                    new ContractAnalysisResult.Finding(
                            RiskCategory.TERMINATION, 20, true,
                            "Paid for work done on termination",
                            "Section 9.3",
                            "Upon termination, Client shall pay for all Services performed through the termination date.",
                            "You are paid for completed work even if the project is cancelled.",
                            null)));

    @Override
    public ContractAnalysisResult analyze(String contractText) {
        return RESULT;
    }

    @Override
    public String modelName() {
        return MODEL_NAME;
    }
}
