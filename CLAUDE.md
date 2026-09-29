# CLAUDE.md: Clausify

Shared instructions for every Claude Code session in this repo. Personal preferences belong in your own `~/.claude/CLAUDE.md` or in `CLAUDE.local.md` (gitignored), not here.

## What this project is

Clausify is an AI contract-analysis web app for freelancers: upload a contract PDF, get a risk score, flagged clauses, and suggested revisions. It is also the semester-long group final project for Enterprise Application Development at the University of Cincinnati. Team: Kymani Jarrett, Ashton Cashier, Venkat Yuva Raaj Narra, Rival Young. The instructor grades Spring Boot layering (controllers, services, repositories), REST APIs, database integration, security, and testing.

**One GitHub repo is submitted for every course lab and the final project.** Each lab submission is marked with a git tag so the grader can see exactly what was submitted, even after later commits.

## Hard constraints

1. **Never edit an applied migration.** V1 to V3 (`users`, `addresses`, `profiles`) were submitted for a graded lab. Every schema change is a new `V<n>__description.sql`. `spring.jpa.hibernate.ddl-auto` stays `validate`; if validation fails, report the exact error instead of changing it.
2. **Never move, delete, or re-point a tag** listed in the README's "Course submissions" table. Never rewrite history on `main` or force-push it.
3. **No secrets in git.** Credentials and keys come from environment variables only; `.env.example` lists them with placeholders.
4. **Ask before** destructive git operations, deleting files outside the task, adding dependencies, or creating cloud resources (Aiven, Render, and Vercel are set up by a human).

## Architecture (decided; reasons in `docs/adr/`)

| Area | Decision |
|---|---|
| Frontend | Angular + Tailwind CSS, npm (not started) |
| Backend | Spring Boot 4.1.x, Java 21 target, Maven |
| Database | MySQL. Local: Docker 8.4 (`docker-compose.yml`). Deployed: Aiven for MySQL free tier (powers off when idle; wake it before demos). |
| Migrations | Flyway (`spring-boot-starter-flyway` + `flyway-mysql`; `flyway-core` alone is not auto-configured in Boot 4) |
| Persistence | Spring Data JPA (Hibernate 7), Lombok. IDs are `BIGINT AUTO_INCREMENT`. |
| File storage | None. PDFs are never persisted; only extracted text is stored. |
| PDF | Apache PDFBox (text extraction, report generation) |
| LLM | Groq `openai/gpt-oss-120b` via Spring AI's OpenAI starter, base URL `https://api.groq.com/openai/v1` (per the Spring AI 2.0 Groq docs), model from `GROQ_MODEL` |
| Embeddings | Spring AI ONNX Transformers, `all-MiniLM-L6-v2`, 384 dims, in the JVM |
| Vector search | Cosine similarity in Java (MySQL Community's `DISTANCE()` is HeatWave-only). Embeddings stored as JSON (`VECTOR` needs MySQL 9.0+; see ADR 0003). |
| Auth | Spring Security, BCrypt, stateless JWT |
| Hosting | Angular on Vercel; API on Render free tier (Docker; sleeps when idle, disk not persistent) |

### Upload flow

1. `POST /api/contracts` receives a multipart PDF (PDF only, max 10 MB).
2. PDFBox extracts text from the in-memory bytes in that request. The file is never written to disk or any store.
3. No usable text (scanned or corrupt PDF): respond `422` with a clear message.
4. Otherwise save a `contracts` row with the text and status `UPLOADED`, return `202`, and run `@Async` analysis: `UPLOADED → ANALYZING → ANALYZED / FAILED`.
5. Retry, comparison, and reports all work from the stored text.

Do not build a file-storage abstraction until retaining files is actually needed (see ADR 0005).

## Code layout and rules

```
backend/src/main/java/com/clausify/
  user/  contract/  analysis/  clause/  config/  common/     package-by-feature
backend/src/main/resources/
  application.yml, application-local.yml, application-prod.yml
  db/migration/V<n>__description.sql                           one change per file
frontend/                                                      Angular (later)
legacy/nextjs-prototype/                                       delete once Angular exists
docs/adr/                                                      architecture decisions
```

- **Layering:** controllers only translate HTTP; business rules live in `@Service` classes; persistence goes through Spring Data repositories. Controllers take and return DTOs, never entities.
- **Ownership:** MySQL has no row-level security, so every query for user-owned data is scoped to the current user in the repository or service (`findByIdAndOwnerId`). Another user's record returns `404`, not `403`.
- **Constructor injection** only, no field `@Autowired`.
- **Lombok on entities:** relationship fields get `@ToString.Exclude` and `@EqualsAndHashCode.Exclude`; passwords are always `@ToString.Exclude`.
- **Tests** alongside each feature: JUnit 5, MockMvc, Testcontainers MySQL.

## Running locally

```bash
docker compose up -d                  # MySQL 8.4 on host port 3307 (MYSQL_PORT to change)
cd backend && ./mvnw spring-boot:run  # "local" profile by default; Flyway migrates on startup
cd backend && ./mvnw verify           # build and run tests
docker exec -it clausify-mysql mysql -uroot -proot clausify   # SQL shell
```

The `local` profile needs no environment variables. The deployed API runs with `SPRING_PROFILES_ACTIVE=prod` and every value from `.env.example` set in Render.

## Workflow

Full details in `CONTRIBUTING.md`.

- Create a GitHub issue, then a branch `type/<issue>-short-desc` (e.g. `feat/12-jwt-auth`).
- Commits and PR titles use Conventional Commits: `type(optional-scope): imperative summary`. Types: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `ci`, `build`, `perf`, `style`.
- Open a PR into `main` (`Closes #<issue>`); CI and the PR-title check must pass and one teammate approves. PRs are merged with a merge commit.
- **Tagging a lab submission:** on `main` after the lab's PR merges, `git tag -a lab-<name> -m "Lab: <title> (submitted)"`, `git push origin lab-<name>`, then add a row to the README's "Course submissions" table in a follow-up PR.

## Up next (do not build until the detailed plan arrives)

The Weeks 2-3 window ends Oct 4. Order:

1. **Auth:** `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`, BCrypt, JWT filter. Reuse the lab's `users` table (new columns via V4 if needed).
2. **Contracts:** V5 `contracts` table (owner FK, filename, size, page count, extracted text, status, timestamps, failure reason); upload endpoint per the upload flow; `GET /api/contracts`, `GET /api/contracts/{id}`, `DELETE`, `POST /{id}/retry`.
3. **Analysis:** `analyses` and `flagged_clauses` tables; Groq prompt returning structured JSON over seven categories (payment terms, liability, IP rights, termination, non-compete, confidentiality, jurisdiction) with 0 to 100 clause scores plus an overall score and High/Medium/Low; `@Async` runner; `GET /api/contracts/{id}/analysis`.
4. Tests with each slice; springdoc-openapi for Swagger UI.
