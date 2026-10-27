# 9. In-memory token-bucket rate limiting

- **Status:** Accepted
- **Date:** 2026-10-27

## Context

The API is public. Login and register can be abused for password guessing and mass sign-ups, and every upload or retry starts a Groq call against a free-tier quota (ADR 0006). Requests reach the API through Vercel's proxy (ADR 0008), but the Render URL is also reachable directly. The API runs as a single Render instance.

## Decision

- A small **token bucket** in `common/RateLimiter`: each key holds up to `capacity` tokens that refill continuously over `period`. Over the limit returns **429** ProblemDetail with a `Retry-After` header.
- Limits (configured in `application.yml` under `clausify.rate-limit`):
  - login: 10 per minute per IP
  - register: 20 per hour per IP (generous enough for a classroom on one network)
  - failed logins: 10 per 15 minutes per email, from any IP
  - uploads and retries: 20 per hour per user (they share one budget because both start an analysis)
- Buckets live **in memory**. No new dependency (Bucket4j was considered): the logic is about 50 lines and fully unit-tested.
- Off in the `local` profile; `RateLimitIntegrationTest` enables it with small limits.

## Consequences

- **Single instance only.** Each instance would count separately if Render ran several, and limits reset on restart or redeploy. Scaling out needs a shared store (Redis, or Bucket4j with a JCache/Redis backend).
- **Per-IP limits are best-effort.** The client IP comes from `X-Forwarded-For`, which Vercel overwrites with the real client IP. A caller that skips Vercel and hits Render directly can forge it. The per-user and per-email limits can't be bypassed that way. Locking Render to Vercel traffic with an origin secret would make per-IP limits reliable (see ADR 0008).
- **Account lockout trade-off.** Anyone can use up an account's failed-login budget, which blocks that account's logins for up to 15 minutes. That's the usual cost of stopping distributed password guessing; the window is short and nothing is permanent.
- Multipart uploads are parsed before the limit check runs, so a rejected upload still costs its transfer (capped at 10 MB).
