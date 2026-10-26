# Clausify codebase guide

Start here. This page tells you where everything lives, what is built, what is not, and who is working on what. It is written for teammates and for Claude Code sessions.

- **Rules and conventions:** [`CLAUDE.md`](../CLAUDE.md) and [`CONTRIBUTING.md`](../CONTRIBUTING.md)
- **Requirements, API table, schema plan:** [`DESIGN.md`](../DESIGN.md) (FR-1 to FR-12, section 7.4 API, 7.5 database)
- **Live task status:** the [project board](https://github.com/users/kymanirjarrett/projects/6). If this page and the board disagree, the board wins.

_Last updated Oct 26, 2026, against `main` at `ecda76f`._

---

## 1. Repo at a glance

```
clausify/
├── backend/                Spring Boot API (Java 21, Maven)
├── frontend/               Angular 22 app (Tailwind v4)
├── docs/
│   ├── adr/                Architecture decision records (why we chose X)
│   ├── design/             Early wireframes and mockups (which screens exist, not how they look)
│   └── CODEBASE.md         This file
├── legacy/nextjs-prototype/  Old prototype. Reference only, do not edit. Will be deleted.
├── .github/workflows/      CI (ci.yml) and PR title check (pr-title.yml)
├── docker-compose.yml      Local MySQL 8.4 on port 3307
├── .env.example            Every env var the deployed API needs (placeholders only)
├── DESIGN.md               Course design document (requirements, diagrams, API, schema)
├── CLAUDE.md               Rules for Claude Code and humans alike
└── CONTRIBUTING.md         Branch, commit, and PR workflow
```

## 2. Backend: where things go

Code is **packaged by feature**: each folder under `backend/src/main/java/com/clausify/` holds everything for one area (entity, repository, service, controller, DTOs). Every request flows **Controller → Service → Repository**:

| Layer | Job | Must not |
|---|---|---|
| Controller | Translate HTTP to a service call; take and return DTOs (Java `record`s) | Contain business rules or touch repositories |
| Service | Business rules, ownership checks, transactions | Return entities to the controller |
| Repository | Spring Data JPA interface | Contain logic |

### Packages and owners

| Package | Owner | Status | What's in it |
|---|---|---|---|
| `user/` | Kymani | Built | `User`, `Address`, `Profile` (+ repositories, from the Flyway lab), `AuthController`, `AuthService`, `JwtService`, DTOs `RegisterRequest`, `LoginRequest`, `AuthResponse`, `UserResponse` |
| `contract/` | Ashton | Partly built | `Contract`, `ContractStatus`, `ContractRepository`, `ContractService`, `ContractController`, `PdfTextExtractor`, `ExtractedDocument`, `ContractUploadedEvent`, DTOs `ContractSummaryResponse`, `ContractDetailResponse`. Still to add: delete, retry, `ReportService`, and Venkat's `ComparisonService` |
| `analysis/` | Rival | **Not started** | Planned: `Analysis`, `ClauseFinding`, `RiskCategory`, `RiskLevel`, `AnalysisService`, `ContractAnalyzer`, `GroqContractAnalyzer`, `RiskScorer`, prompt templates |
| `clause/` | Venkat | **Not started** | Planned: `StandardClause`, `StandardClauseRepository`, `ClauseController`, `ClauseSimilarityService`, `EmbeddingService`, `StandardClauseLoader`, seed data |
| `config/` | Shared | Built | `SecurityConfig` (which URLs are public), `JwtConfig`, `JwtProperties`, `CorsProperties`, `OpenApiConfig` (Swagger), `ProblemDetailAuthenticationEntryPoint` |
| `common/` | Shared | Built | `GlobalExceptionHandler` (turns exceptions into RFC 9457 error JSON), `NotFoundException`, `ConflictException`, `UnreadablePdfException`, `UnsupportedFileTypeException`, `PageResponse`, `CurrentUser` |

### Recipes

**Add an endpoint for user-owned data**

1. Controller method takes `@AuthenticationPrincipal Jwt jwt` and passes `CurrentUser.id(jwt)` to the service. See `ContractController`.
2. Service loads with an owner-scoped query, e.g. `contracts.findByIdAndUserId(id, userId)`, and throws `NotFoundException` if empty. Another user's record must give the **same 404** as a missing one (MySQL has no row-level security, so this is our only isolation).
3. Throw `ConflictException` for an invalid state change (409). `GlobalExceptionHandler` formats the error; you don't build error JSON yourself.
4. Add `@Operation` / `@Tag` annotations so it shows in Swagger at `http://localhost:8080/swagger-ui.html`.
5. Public endpoints must be added to `SecurityConfig`; everything else requires a JWT by default.

**Kick off background work after upload.** `ContractService` publishes `ContractUploadedEvent(contractId)` after saving. The FR-5 analysis listener (not built yet) should handle it with `@TransactionalEventListener` + `@Async`, so the upload returns `202` without waiting for the AI.

**Change the schema.** Add a new file `backend/src/main/resources/db/migration/V<n>__description.sql`. Never edit an existing migration (V1 to V3 were graded). Hibernate only *validates* the schema (`ddl-auto: validate`), so your entity must match the SQL exactly or the app won't start.

### Config and profiles

| File | Used when | Notes |
|---|---|---|
| `application.yml` | Always | Shared settings: Flyway on, `ddl-auto: validate`, 10 MB upload cap, max page size 100 |
| `application-local.yml` | Default on your machine | Points at Docker MySQL; needs no env vars |
| `application-prod.yml` | Render (`SPRING_PROFILES_ACTIVE=prod`) | Everything from env vars: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `GROQ_API_KEY`, `GROQ_MODEL` |

## 3. Database

Flyway runs every migration on startup. Migration numbers are **reserved** and must merge **in order** (Flyway rejects V7 if V6 isn't there yet).

| Migration | Tables | Owner | Status |
|---|---|---|---|
| V1 to V3 | `users`, `addresses`, `profiles` | Kymani (lab) | Applied, frozen |
| V4 | `users` unique email + `created_at` | Kymani | Applied |
| V5 | `contracts` (FK `user_id`, cascade delete, index `(user_id, uploaded_at)`) | Ashton | Applied |
| V6 | `analyses` (one per contract), `clause_findings` (many per analysis) | Rival | **Not started** (#34) |
| V7 | `standard_clauses` + seed data | Venkat | **Not started** (#50) |

### Entity relationships

| Relationship | Type | Status |
|---|---|---|
| `User` → `Address` | one-to-many | Built |
| `User` → `Profile` | one-to-one | Built |
| `Contract` → `User` | many-to-one | Built |
| `Analysis` → `Contract` | one-to-one | Planned (V6) |
| `ClauseFinding` → `Analysis` | many-to-one | Planned (V6) |
| `StandardClause` | standalone, compared by embedding | Planned (V7) |

Embeddings are stored as JSON and compared with cosine similarity **in Java**, because MySQL Community can't do vector math ([ADR 0003](adr/0003-mysql-with-java-cosine-similarity.md)). PDFs are never stored, only their extracted text ([ADR 0005](adr/0005-extract-text-do-not-store-pdfs.md)).

## 4. REST API status

Full table with request/response details: [DESIGN.md section 7.4](../DESIGN.md#74-rest-api). Try built endpoints in Swagger UI.

| Endpoint | FR | Owner | Status |
|---|---|---|---|
| `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` | FR-1, FR-2 | Kymani | ✅ Built |
| `POST /api/contracts` (upload) | FR-3 | Ashton | ✅ Built |
| `GET /api/contracts`, `GET /api/contracts/{id}` | FR-4 | Ashton | ✅ Built |
| `DELETE /api/contracts/{id}` | FR-8 | Ashton | Not started (#48) |
| `POST /api/contracts/{id}/retry` | FR-7 | Rival | In progress (PR #84) |
| `GET /api/contracts/{id}/analysis` | FR-5, FR-6 | Rival | Not started (#42) |
| `GET /api/standard-clauses`, `GET /api/clauses/{findingId}/similar` | FR-9 | Venkat | Not started (#50) |
| `POST /api/contracts/{id}/compare/{otherId}` | FR-10 | Venkat | Not started (#51) |
| `GET /api/contracts/{id}/report` | FR-11 | Ashton (proposed) | Not started (#52) |
| `GET /api/users/me/stats`, `PATCH /api/users/me` | FR-12 | Kymani (proposed) | Not started (#57) |

Errors are always RFC 9457 `ProblemDetail` JSON: 400 validation, 401 no/bad token, 404 missing **or not yours**, 409 conflict, 413 too large, 415 not a PDF, 422 unreadable PDF.

## 5. Frontend: where things go

Angular 22 with standalone components, signals, and no Zone.js. Code lives in `frontend/src/app/`:

| Folder | What's in it |
|---|---|
| `app.routes.ts` | All routes. `/`, `/login`, `/signup` are public; everything under `/app` needs sign-in (`signedInGuard`) |
| `core/` | App-wide services: `auth.service.ts` (token + current user), `auth.interceptor.ts` (adds `Authorization` to `/api/` calls), `auth.guards.ts`, `contracts.service.ts`, `api.models.ts` (TypeScript shapes of API responses), `problem.ts` (reads error JSON) |
| `shared/` | Small reusable pieces: `icon`, `wordmark`, `status-stamp`, `format`, `breakable` |
| `features/landing/` | Landing page |
| `features/auth/` | Login and sign-up |
| `features/shell/` | Signed-in layout wrapping the `/app` pages |
| `features/dashboard/` | Contract list + upload sheet (FR-3, FR-4) |
| `features/contract/` | Contract detail. Analysis area is drawn as "ghost" placeholders until FR-6 exists |
| `features/account/` | Account page |

**Where new screens go:** Analysis Results (#55, Rival) extends `features/contract/`. Clause and Version Comparison (#56, Venkat) go in a new `features/comparison/` with routes under `/app`. Add API calls to a service in `core/` and response types to `api.models.ts`.

**UI rules:** follow `frontend/DESIGN.md` (visual system) and `frontend/PRODUCT.md`. Never show a feature as working before its API exists; use the ghost/absent states. The app calls relative `/api/...` URLs; locally `proxy.conf.json` forwards them to `:8080`.

## 6. Tests and CI

| What | Where | Run |
|---|---|---|
| Backend unit + integration tests | `backend/src/test/java/com/clausify/<package>/` (mirrors main) | `cd backend && ./mvnw verify` (needs Docker running for Testcontainers MySQL) |
| Test PDFs | `backend/src/test/resources/contracts/` (`fair-nda.pdf`, `risky-consulting-agreement.pdf`, `scanned-image-only.pdf`) | Regenerate with `generate_fixtures.py` |
| Frontend unit tests | `*.spec.ts` next to the file they test | `cd frontend && npx ng test --watch=false` |
| Coverage | JaCoCo report, uploaded as a CI artifact | `backend/target/site/jacoco/index.html` |

Test naming patterns already used: `*Test` for unit/slice tests, `*IntegrationTest` for full-stack tests with MockMvc + Testcontainers. Copy an existing one from `contract/` as a starting point.

**Test files go in `src/test/java`, never `src/main/java`.** Test libraries aren't on the main classpath, so tests in `src/main` break the build.

CI (`.github/workflows/ci.yml`) runs on every PR: `backend` (`./mvnw verify`) and `frontend` (tests + production build). `pr-title.yml` fails the PR unless the title is Conventional Commits, e.g. `feat(contract): add delete endpoint`.

## 7. Workflow in 30 seconds

1. Pick your issue on the board (`gh issue view <n>` for details).
2. Branch from fresh `main`: `git switch main && git pull && git switch -c feat/<issue>-short-desc`.
3. Commit with Conventional Commits: `feat(clause): add standard clause entity`.
4. Open a PR into `main` with `Closes #<issue>` in the body and a Conventional Commits title. CI must be green before merge.

## 8. What's outstanding

Sprint 1 (due Oct 11) and Sprint 2 (due Oct 25) both have open work; Sprint 3 ends Nov 8. The analysis pipeline (FR-5) is the critical path: FR-6, FR-7, FR-9 similarity, FR-10 risk deltas, FR-11 reports, and the Analysis Results screen all read its data.

### For the Part 3 progress report

The assignment expects these to be **complete** at report time:

| Requirement | Status | Gap and owner |
|---|---|---|
| Database schema fully implemented | V1 to V5 done | **V6 (Rival, #34) and V7 (Venkat, #50)** |
| All entities mapped | `User`, `Address`, `Profile`, `Contract` | **`Analysis`, `ClauseFinding` (Rival, #35), `StandardClause` (Venkat)** |
| All relationships working | 4 of 6 | Analysis↔Contract, ClauseFinding→Analysis (Rival) |
| Repositories for all entities | 4 of 7 | Come with the entities above |
| Some controllers, DTOs, APIs | ✅ Auth and Contracts (6 endpoints) | Already satisfied |

If V6 and V7 aren't merged by the report deadline, say so in the report's Challenges section with the catch-up plan below.

### Who works on what (Oct 26 to Nov 15)

"Proposed" means the issue isn't assigned on the board yet.

**Rival (AI analysis):** critical path, so V6 goes first.
- Week 1: #34 V6 migration, #35 entities (merge these first; they unblock Venkat's V7), #36 Groq config, #37 analyzer, #38 `RiskScorer`
- Week 2: #39 `AnalysisService` + #40 tests, #42/#43 analysis endpoint, finish FR-7 (#45/#46, PR #84)
- Week 3: #55 Analysis Results screen; #64 AI evaluation harness (proposed)

**Venkat (clause library and comparison):**
- Week 1: FR-9 part 1 (#50): `StandardClause` entity, repository, seed data, `GET /api/standard-clauses` with category filter. Write V7 now but merge it **after** Rival's V6
- Week 2: FR-9 part 2: `EmbeddingService` (ONNX MiniLM), cosine similarity, `GET /api/clauses/{findingId}/similar` (needs FR-5 findings to exist)
- Week 3: #56 Clause and Version Comparison screens; FR-10 (#51, a "Could" priority, so it can slip)

**Ashton (contracts and documents, Scrum Master):**
- Week 1: #48 `DELETE /api/contracts/{id}` + #49 tests
- Week 2: #52 FR-11 PDF report with PDFBox (proposed; needs FR-5 data, so start with layout using fake data)
- Week 3: Dashboard follow-ups (delete and retry buttons); keep the board current

**Kymani (accounts, security, deployment):**
- Week 1: #58 E-2 deploy phase 1 (Vercel); #80 rate limiting on auth and upload (proposed)
- Week 2: E-2 phase 2 (Render + Aiven); #57 FR-12 account stats and profile update (proposed)
- Week 3: #59 E-3 coverage + Playwright (shared, proposed); #60 E-4 final docs (shared)

**Everyone:** for the Part 3 report, send code snippets and Postman screenshots for your own area's entities, repositories, controllers, and endpoints.

## 9. Gotchas

- **Migration order:** V6 before V7. If you need a schema change outside your reserved number, take the next free number after V7 and tell the team.
- **Aiven requires primary keys** on every table (`sql_require_primary_key`). Every new table needs one.
- **Don't commit `frontend/angular.json` analytics IDs.** The Angular CLI adds an `"analytics"` entry if you accept its prompt; discard that change.
- **Ownership tests:** every new endpoint on user data needs a test that another user gets 404. Copy `ContractQueryIntegrationTest`.
- **The AI is slow and can fail.** Never call Groq inside a request; do it in the async listener and set the contract to `FAILED` with a readable `failureReason` on errors.
