# Specification

The rules the code implements, kept next to the code that cites them. Architecture decisions and their trade-offs are in [`adr/`](adr/).

## Risk scoring

The model scores each clause (0 to 100), but the **overall score is computed in Java**, not by the model, so it is consistent and unit-testable:

- Using only the non-favorable findings: `overall = round(0.6 × highest score + 0.4 × average score)`. Weighting the highest score means one severe clause cannot be averaged away by many mild ones.
- If there are no non-favorable findings, the overall score is 0.
- Levels: **Low** below 40, **Medium** 40 to 69, **High** 70 and above.

Example: findings of 85, 78, 55 (risky) and 20 (favorable) give `0.6 × 85 + 0.4 × 72.7 = 80`, which is **High**.

## AI analysis contract

`GroqContractAnalyzer` sends a system prompt that defines the 8 categories, the scoring scale, and a required JSON shape, then the contract text. Spring AI maps the reply to a Java record, and invalid output fails the analysis cleanly (status `FAILED`) instead of saving partial data.

```json
{
  "contractType": "Consulting Agreement",
  "summary": "Concentrated risk in liability and payment terms.",
  "findings": [
    {
      "category": "LIABILITY",
      "riskScore": 85,
      "favorable": false,
      "title": "Unlimited Liability Exposure",
      "sectionRef": "Section 7.2",
      "originalText": "Contractor shall indemnify and hold harmless the Client from any and all damages...",
      "explanation": "There is no cap on damages you could owe.",
      "suggestedRevision": "Contractor's total liability shall not exceed the fees paid in the preceding 3 months."
    }
  ]
}
```

Similarity search embeds each finding's original text and every standard clause with the same word-based hashing embedder (ADR 0012), then ranks standard clauses **in the same category** by cosine similarity and returns the top 3 (shown as a percentage).

## Evaluation

Automated tests prove that the code works, but they cannot prove that the model's findings are right. Model accuracy is measured separately, against contracts whose expected findings are written down before the model sees them.

**Evaluation set: 40 contracts**

| Source | Contracts | Why |
|---|---|---|
| [CUAD](https://www.atticusprojectai.org/cuad) (Contract Understanding Atticus Dataset): 510 commercial contracts with 13,000+ labels by experienced lawyers, CC BY 4.0 | 25, chosen from services, consulting, licensing, and supply agreements | Real-world contract language, with expert labels that locate clauses for 5 of our 8 categories: *Uncapped Liability* and *Cap on Liability* (Liability), *Non-Compete*, *IP Ownership Assignment* (IP Rights), *Termination for Convenience* (Termination), and *Governing Law* (Jurisdiction) |
| Synthetic freelancer contracts written for this project | 15 | Cover the categories CUAD does not label (Payment Terms, Indemnification, Confidentiality) and freelancer-specific risks, with known risky and fair clauses planted on purpose |

Contracts average about four risky clauses each. No real client contracts are used.

**How expected results (gold labels) are established**

1. A written labeling guide defines each category and the Low, Medium, and High levels, using the same definitions as the analysis prompt (see [AI analysis contract](#ai-analysis-contract)).
2. Two labelers label every contract independently: for each relevant clause, its category, whether it is risky or favorable for the freelancer, and its expected level.
3. For CUAD contracts, the lawyers' annotations identify where each clause is and what type it is. The labelers add the freelancer-risk judgment, because CUAD records that a clause exists, not whether it hurts the contractor.
4. Disagreements are resolved in discussion and the decision is noted. Agreement before discussion is reported as Cohen's kappa, so readers can judge how reliable the labels are.
5. Labels are committed to the repository as JSON (`backend/src/test/resources/eval/`) and frozen before the model is run on the test split.

**Splits.** 10 contracts form a development set for tuning the prompt. The other **30 are a held-out test set (about 120 risky clauses)** used only for reported results, so the prompt is never tuned to the answers it is graded on.

**Matching and metrics.** A model finding matches a gold clause when the categories are equal and the finding's quoted text covers at least half of the gold clause's words (or the section references match).

| Metric | Definition | Target |
|---|---|---|
| Recall (primary) | matched gold risky clauses ÷ all gold risky clauses | ≥ 85% |
| Precision | risky findings that match a gold clause ÷ all risky findings (stops the model meeting recall by flagging everything) | ≥ 75% |
| Per-category recall | recall within each of the 8 categories, so one weak category cannot hide in the average | reported |
| Level agreement | matched findings whose level equals the gold level, and within one level | reported |
| Overall level agreement | contracts whose overall level (see [Risk scoring](#risk-scoring)) equals the level computed from the gold labels | reported |

**Why the size matters.** The target is measured over clauses, not contracts. At an observed recall of 85%, 10 contracts (about 40 clauses) give a 95% confidence interval of roughly 71% to 93%, which cannot tell an 85% model from a 75% one. About 120 held-out clauses narrow it to roughly 78% to 90%. Every metric is reported with its Wilson 95% confidence interval alongside the point estimate.

**Running the evaluation.**

- An evaluation harness (a JUnit test tagged `@Tag("eval")`, excluded from CI because it calls Groq and uses the free-tier quota) runs the real `GroqContractAnalyzer` over the test split.
- Each contract is analyzed 3 times at low temperature; the report shows the mean and spread, because model output varies between runs.
- Each run writes a report to [`docs/evaluation/`](evaluation/) with the date, model name, prompt version, and every metric with its interval. It is re-run after every prompt or model change, so results can be compared over time.

---
