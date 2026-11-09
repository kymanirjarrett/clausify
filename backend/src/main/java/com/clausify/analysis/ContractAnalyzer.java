package com.clausify.analysis;

/**
 * Reads a contract's text and returns its risky and favorable clauses (DESIGN.md 7.7).
 *
 * <p>Two implementations: {@code GroqContractAnalyzer} in the {@code prod} profile (the only class allowed to
 * call an LLM) and {@link FakeContractAnalyzer} everywhere else, so local runs and tests need no API key.
 * Callers depend on this interface, never on a specific implementation.
 */
public interface ContractAnalyzer {

    /**
     * @param contractText text extracted from the uploaded PDF; untrusted input
     * @return validated findings; the overall score is computed separately by {@code RiskScorer}
     * @throws ContractAnalysisException if the analysis cannot produce a valid result
     */
    ContractAnalysisResult analyze(String contractText);

    /** Recorded on each {@code analyses} row so results can be traced to the model that produced them. */
    String modelName();
}
