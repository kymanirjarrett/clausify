# AI evaluation: labeling guide

This guide is for the two people who label the evaluation contracts (docs/spec.md, Evaluation, #64). Label each contract **on your own first**, without looking at the other labeler's work or at the model's output. Agreement before discussion is reported as Cohen's kappa, so independent first passes matter.

The categories and levels below are the same definitions the analysis prompt gives the model (`backend/src/main/resources/prompts/contract-analysis-system.md`). If the prompt changes, update this guide in the same pull request.

## What to label

Read the contract as the **freelancer or contractor**. For every clause that puts the freelancer at risk, or clearly works in their favor, record one label:

| Field | Values | Notes |
|---|---|---|
| `key` | the clause's section number, e.g. `7.2` | Use the number as printed. With no number, use the heading, e.g. `Governing Law`. Both labelers must use the same convention, because labels are paired by key. |
| `category` | one of the 8 below | One category per clause. If a clause covers two topics, split it into two labels with the same key. |
| `favorable` | `true` or `false` | `true` only for a clause that clearly benefits the freelancer. |
| `level` | `LOW`, `MEDIUM`, `HIGH` | The risk to the freelancer. For a favorable clause, use its remaining risk, usually `LOW`. |

Skip boilerplate that carries no risk either way (notices, counterparts, headings).

## Categories

- **PAYMENT_TERMS**: payment amount, schedule, invoicing, late fees, kill fees, expenses.
- **LIABILITY**: limits or caps on damages, consequential damages, warranties.
- **INDEMNIFICATION**: who defends and pays for third-party claims, and whether it is mutual.
- **IP_RIGHTS**: ownership and licensing of work product, work made for hire, pre-existing IP, portfolio rights.
- **TERMINATION**: how and when either side can end the contract, notice periods, payment on termination.
- **NON_COMPETE**: non-compete, non-solicitation, exclusivity.
- **CONFIDENTIALITY**: what must be kept secret, for how long, and exceptions.
- **JURISDICTION**: governing law, venue, arbitration, dispute resolution.

## Levels

- **HIGH** (the model scores these 70 to 100): could cause serious financial or legal harm. Examples: uncapped liability, one-way indemnification, all IP assigned before payment, a broad non-compete.
- **MEDIUM** (40 to 69): unfavorable but common, or a risk limited in scope. Examples: net-60 payment, a short notice period.
- **LOW** (0 to 39): minor, or typical for this kind of contract.

**Favorable** examples: a liability cap, late fees owed to the freelancer, payment for work completed on termination, retained portfolio rights.

## CUAD contracts

CUAD's lawyer annotations tell you **where** a clause is and **what type** it is. They do not say whether it hurts the contractor, so you still make the risk call. Map CUAD labels like this:

| CUAD label | Our category |
|---|---|
| Uncapped Liability, Cap on Liability | LIABILITY |
| Non-Compete | NON_COMPETE |
| IP Ownership Assignment | IP_RIGHTS |
| Termination for Convenience | TERMINATION |
| Governing Law | JURISDICTION |

Read the whole contract anyway: clauses in our other three categories (payment, indemnification, confidentiality) count too.

## Reconciling

After both passes, meet and agree on one **gold** list per contract. For each gold clause, copy the clause text exactly as it appears (the matcher compares words, so a paraphrase can fail to match). Write how each disagreement was settled in the file's `notes`.

## File format

One pair of files per contract in `backend/src/test/resources/eval/<split>/`, where `<split>` is `dev` (10 contracts, for prompt tuning) or `test` (30 held-out contracts, for reported results):

- `<id>.txt`: the contract text, as Clausify would extract it.
- `<id>.json`:

```json
{
  "id": "syn-01",
  "source": "SYNTHETIC",
  "sourceRef": null,
  "labelerA": [{ "key": "7.2", "category": "LIABILITY", "favorable": false, "level": "HIGH" }],
  "labelerB": [{ "key": "7.2", "category": "LIABILITY", "favorable": false, "level": "MEDIUM" }],
  "gold": [
    {
      "sectionRef": "7.2",
      "category": "LIABILITY",
      "favorable": false,
      "level": "HIGH",
      "text": "Contractor shall be liable for any and all damages arising from the Services."
    }
  ],
  "notes": "7.2: A said HIGH, B said MEDIUM; agreed HIGH because the liability is uncapped."
}
```

`source` is `SYNTHETIC` or `CUAD`; for CUAD, `sourceRef` is the CUAD file name. Labels are **frozen** once the test split is first run: after that, change them only in a pull request that says why.

## Running

See [README.md](README.md).
