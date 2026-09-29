# Architecture Decision Records

An Architecture Decision Record (ADR) is a short document that captures one important technical decision: the situation that forced it, what we chose, and what that choice costs us. Code shows *what* the system does; ADRs explain *why* it looks that way, so a new teammate, grader, or future us can understand a decision without digging through chat history.

We use Michael Nygard's format: **Title, Status, Context, Decision, Consequences**. Each record stays under a page.

## Rules

- One decision per file, numbered in order: `NNNN-short-title.md`.
- Accepted ADRs are not rewritten. To change a decision, add a new ADR and mark the old one `Superseded by NNNN`.
- Add an ADR when a choice is hard to reverse, affects the whole team, or was debated.

## Index

| # | Decision | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-angular-frontend-with-tailwind.md) | Angular frontend with Tailwind CSS | Accepted |
| [0003](0003-mysql-with-java-cosine-similarity.md) | MySQL with cosine similarity in Java | Accepted |
| [0004](0004-aiven-free-tier-for-hosted-mysql.md) | Aiven free tier for hosted MySQL | Accepted |
| [0005](0005-extract-text-do-not-store-pdfs.md) | Extract text, do not store PDFs | Accepted |
| [0006](0006-groq-via-spring-ai-openai-starter.md) | Groq via Spring AI's OpenAI starter | Accepted |
| [0007](0007-stateless-jwt-authentication.md) | Stateless JWT authentication | Accepted |
