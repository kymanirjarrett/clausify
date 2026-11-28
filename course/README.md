# Course materials

Files that exist only for the semester's group project and team workflow. None of the app's code depends on them, so this folder can be deleted once grading is finished.

| Path | What it is |
|---|---|
| `DESIGN.md` | Submitted design document: requirements FR-1 to FR-12, diagrams, API table, schema plan, risk formula, AI contract, evaluation method |
| `design/` | Wireframes and mockups referenced by the design document |
| `DEMO.md` | Five-minute presentation script |
| `CODEBASE.md` | Onboarding guide for teammates |
| `github/` | The team's PR template (inactive here; GitHub only reads it from `.github/`) |
| `adr/0004-*.md` | Superseded decision (Aiven MySQL, replaced by ADR 0011) |

The sections the code relies on (risk scoring, the AI JSON contract, the evaluation method) are copied to `docs/spec.md`, so nothing outside this folder depends on it. Before deleting it, update the two pointers in CLAUDE.md.

## Deleted submission tags

These tags were removed on 2026-11-28. To recreate one: `git tag -a <tag> <commit> -m "<message>"` and `git push origin <tag>`.

| Tag | Commit |
|---|---|
| `design-doc` | `c22e3b88d0f474beb8d073a53b26b07059763704` |
| `lab-flyway-mysql` | `eae32e8d79ca5ccfb5dd5599e9a527770919f267` |
| `v0-nextjs-prototype` | `f00e0269580dcbef69dd6f2714297de60f8231fe` |
