# AI evaluation set

Labeled contracts for the DESIGN.md 7.9 evaluation (#64). How to label and the file format: `docs/evaluation/labeling-guide.md`.

- `dev/`: 10 contracts for prompt tuning.
- `test/`: 30 held-out contracts for reported results. Frozen once first run.
- `sample/`: **not gold data.** One synthetic contract that only exercises the pipeline in CI (`EvaluationPipelineTest`).

`dev/` and `test/` are empty until the team adds labeled contracts.
