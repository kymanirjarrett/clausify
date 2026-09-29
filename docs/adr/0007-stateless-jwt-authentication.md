# 7. Stateless JWT authentication

- **Status:** Accepted
- **Date:** 2026-09-27

## Context

Users must only ever see their own contracts and analyses. The Angular frontend (Vercel) and the API (Render) are on different domains, and Render's free tier can restart or run more than one instance, so any login state kept in server memory can disappear.

## Decision

- **Spring Security** with **BCrypt**-hashed passwords, reusing the lab's `users` table (new columns via new migrations).
- On login, the API issues a signed **JWT**; the Angular `HttpInterceptor` sends it as `Authorization: Bearer <token>`. The API keeps no session (`SessionCreationPolicy.STATELESS`); a filter validates the token on every request. The signing key comes from `JWT_SECRET`.
- **Ownership is enforced on every query** for user data (for example `findByIdAndOwnerId`), because MySQL has no row-level security (ADR 0003).
- **Another user's resource returns `404 Not Found`, not `403 Forbidden`.** A 403 would confirm that the id exists; a 404 reveals nothing.

## Consequences

- Any API instance can serve any request, which suits Render and scales horizontally.
- A JWT cannot be revoked before it expires, so tokens are short-lived; logout is handled by the client discarding the token. Refresh tokens can be added later if needed.
- CORS must allow the Vercel origin (`CORS_ALLOWED_ORIGINS`), and `JWT_SECRET` must be long and random.
- Login and registration should be rate-limited to slow password guessing.
