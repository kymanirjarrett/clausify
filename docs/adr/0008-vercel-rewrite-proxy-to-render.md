# 8. Vercel rewrite proxy to the Render API

- **Status:** Accepted
- **Date:** 2026-10-27

## Context

The Angular app is hosted on Vercel and the API on Render, on different domains. The app already calls relative URLs (`/api/...`), and `auth.interceptor.ts` attaches the token only to paths that start with `/api/`. Calling Render's domain directly from the browser would need an absolute API URL per environment and a CORS preflight before most requests.

## Decision

Vercel **rewrites** (reverse-proxies) these paths to `https://clausify-api.onrender.com`, in `frontend/vercel.json`:

- `/api/health` to `/actuator/health` (the app's warm-up ping)
- `/api/*`
- `/swagger-ui.html`, `/swagger-ui/*`, `/v3/api-docs`, `/v3/api-docs/*` (Swagger is public)

Every other path falls back to `index.html` for Angular's router. Rewrite caching is disabled for `/api/*` (`x-vercel-enable-rewrite-caching: 0`), and Spring Security sends `Cache-Control: no-store` on authenticated responses, so a cached response can never reach another user.

## Consequences

- **No app code changes and no CORS preflight:** the browser sees one origin.
- **One more network hop** through Vercel's edge on every API call.
- **120-second proxy limit:** Vercel gives up on a proxied request after 120 s. Render's free API takes about that long to wake, so the app pings `/api/health` on load and retries through 502s, and the login and signup forms explain the wait (#106).
- **Client IP:** Vercel overwrites `X-Forwarded-For` with the real client IP, so per-IP rate limits work through the proxy (ADR 0009). Render's URL is still public, though, so a direct caller can forge that header.
- **Preview deployments use the production API.** Every PR's Vercel preview calls the same Render service and Aiven database.
- **Possible follow-up:** have Vercel send a shared origin-secret header and reject requests to Render without it, which would lock the API to Vercel traffic and make per-IP limits reliable.
