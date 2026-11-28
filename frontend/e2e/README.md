# End-to-end tests

Playwright runs the main journeys (#59) in Chromium against the real API and database. The API uses the `local`
profile, so analysis comes from the fake analyzer and Groq is never called.

| Test | Covers |
|---|---|
| sign up, upload a contract, and review its analysis | FR-1, FR-3, FR-6 (score, filters), FR-9 (compare to standard), FR-11 (report download) |
| compare two contracts and warn when they are unrelated | FR-10 |
| browse the clause library and see account statistics | FR-9 library, FR-12 |

Each test signs up its own account, so tests never share data.

## Run locally

```bash
docker compose up -d                       # MySQL on 3307
cd backend && ./mvnw spring-boot:run       # API on 8080 (keep it running)
cd frontend && npx playwright install chromium   # once
cd frontend && npm run e2e                 # starts ng serve if it is not already running
```

CI runs the same thing in the `e2e` job (`.github/workflows/ci.yml`). On failure it uploads the Playwright report and
traces, and prints the API log.
