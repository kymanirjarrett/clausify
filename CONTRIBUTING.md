# Contributing to Clausify

How the team gets a change from idea to `main`. Project context and architecture live in [`CLAUDE.md`](CLAUDE.md) and [`docs/adr/`](docs/adr/).

## Setup

Prerequisites: JDK 21 or newer, Docker Desktop, Git. (Node and npm once the Angular app exists.)

```bash
git clone https://github.com/kymanirjarrett/clausify.git
cd clausify
docker compose up -d                  # MySQL 8.4 on localhost:3307
cd backend && ./mvnw spring-boot:run  # API on http://localhost:8080, "local" profile
```

No environment variables are needed locally. If port 3307 is taken, run `MYSQL_PORT=3308 docker compose up -d` and set `DB_URL=jdbc:mysql://localhost:3308/clausify`.

Useful Docker commands: `docker compose ps` (status), `docker compose logs mysql` (logs), `docker compose stop` (stop, keep data), `docker compose down -v` (delete the database; Flyway rebuilds it on the next start).

## Workflow

1. **Open an issue** describing the work, or pick an existing one.
2. **Branch from an up-to-date `main`:** `type/<issue>-short-desc`, for example `feat/12-jwt-auth` or `fix/31-upload-size`.
3. **Commit** in small steps using [Conventional Commits](https://www.conventionalcommits.org/):
   ```
   type(optional-scope): imperative summary
   ```
   - Types: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `ci`, `build`, `perf`, `style`
   - Scope is optional; use the feature area when it fits: `auth`, `user`, `contract`, `analysis`, `clause`, `config`
   - Lowercase after the colon, imperative mood ("add", not "added"), no trailing period, 72 characters max
4. **Open a PR into `main`** with a Conventional Commit title and the template filled in, including `Closes #<issue>`.
5. **Checks must pass:** `backend` (build and tests against MySQL) and `pr-title` (title format).
6. **Review:** one teammate approves. Reviewers check layering, tests, security (ownership checks, no secrets), and that migrations are new files. Resolve every conversation before merging.
7. **Merge** with the "Create a merge commit" button; the merge commit is titled with the PR title. Delete the branch afterwards.

## Code rules

- Controllers translate HTTP only; business logic lives in `@Service` classes; persistence goes through Spring Data repositories. The API takes and returns DTOs, never entities.
- Every query for user-owned data is scoped to the current user. Another user's record returns `404`.
- Constructor injection only.
- **Schema changes are new Flyway migrations** (`V<n>__description.sql`). Never edit a migration that has been applied or pushed.
- Secrets come from environment variables. Add every new variable to [`.env.example`](.env.example) with a placeholder and a one-line comment.
- Significant or hard-to-reverse technical decisions get an ADR in [`docs/adr/`](docs/adr/).

## Tests

```bash
cd backend && ./mvnw verify   # compile, run all tests (needs the Docker MySQL running)
```

Write tests alongside each feature: JUnit 5 for services, MockMvc for controllers, Testcontainers MySQL for repository and integration tests. Name tests after behaviour, e.g. `returnsNotFoundForAnotherUsersContract`.

## Tagging a lab submission

Lab submissions are marked with annotated tags so the grader sees exactly what was submitted.

1. Merge the lab's PR into `main`.
2. Tag the merge commit and push the tag:
   ```bash
   git checkout main && git pull
   git tag -a lab-<name> -m "Lab: <title> (submitted)"
   git push origin lab-<name>
   ```
3. In a follow-up PR, add a row to the README's "Course submissions" table with the tag and key paths.

Never move, delete, or re-point a submission tag.
