# AI evaluation

How accurate Clausify's findings are (goal G2), measured as described in docs/spec.md, Evaluation and tracked in #64.

| File | What it is |
|---|---|
| [labeling-guide.md](labeling-guide.md) | How two team members label contracts, and the JSON file format |
| `backend/src/test/resources/eval/{dev,test}/` | The labeled contracts: `<id>.txt` plus `<id>.json` |
| `backend/src/test/java/com/clausify/eval/` | Metrics (`EvalMetrics`), runner and report (`EvaluationRunner`), and the Groq run (`AiEvaluationTest`) |
| `YYYY-MM-DD-<split>-<model>.md` (this folder) | One report per run |

## Status

- **Built:** labeling guide, file format, metrics, report, and the `@Tag("eval")` run. `EvalMetricsTest` and `EvaluationPipelineTest` check them in CI on hand-worked examples and a one-contract sample (`eval/sample/`, which is not gold data).
- **To do by the team:** choose 25 CUAD contracts and write 15 synthetic ones, label each one twice independently, reconcile, then run the dev split, tune, and run the test split once.

## What each metric means

- **Recall** (target at least 85%): gold risky clauses the model found.
- **Precision** (target at least 75%): the model's risky findings that are real gold clauses. This stops a model from reaching recall by flagging everything.
- **Recall by category:** so one weak category can't hide in the average.
- **Level agreement:** for found clauses, whether the model's level equals the gold level, and whether it's within one level.
- **Overall level agreement:** contracts where the model's overall level (docs/spec.md, Risk scoring) equals the level computed from the gold labels. Gold levels are scored as Low 20, Medium 55, High 85.
- **Cohen's kappa:** how much the two labelers agreed before discussing. Low kappa means the gold labels themselves are shaky.

A finding **matches** a gold clause when the categories and the favorable flag are equal, and either its quote covers at least half of the gold clause's distinct words or the section references match (`Section 7.2`, `§ 7.2` and `7.2.` are the same). Each finding and each gold clause is used at most once, best match first. Every metric is shown per run, as a mean, and with a Wilson 95% interval over all runs pooled. A run where an analysis fails counts that contract's clauses as missed, so failures can't make the score look better.

## Running it

It calls the real Groq model, so it is **excluded from every normal build and from CI**. It needs `GROQ_API_KEY`, and without one it skips.

```bash
cd backend && GROQ_API_KEY=... ./mvnw test -Dsurefire.excludedGroups= -Dgroups=eval -Deval.split=dev -Deval.runs=3
```

The report is written to this folder. Commit it with a note on what changed since the last run (the prompt version in the report is a fingerprint of `backend/src/main/resources/prompts/`).

**Quota.** Groq's free tier on `gpt-oss-120b` allows about 200K tokens a day, and one analysis request uses up to about 6,400 tokens. Three runs of the 10-contract dev split take about 380K tokens. The 30-contract test split takes about 1.15M, so spread it over several days with `-Deval.runs=1`, and only once the labels are frozen.
