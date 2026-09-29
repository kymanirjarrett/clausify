# 3. MySQL with cosine similarity computed in Java

- **Status:** Accepted
- **Date:** 2026-09-27

## Context

The prototype used Supabase Postgres with pgvector to compare contract clauses against standard clauses. The course requires MySQL for its labs, and the graded Flyway lab (`users`, `addresses`, `profiles`) already lives in this repo, so the project must run on MySQL.

MySQL has a `VECTOR` type, but only from version 9.0, and the function that compares vectors, `DISTANCE()`, is "available only for users of MySQL HeatWave on OCI and MySQL AI; it is not included in MySQL Commercial or Community distributions" (MySQL reference manual). Our local database is MySQL 8.4, and the hosted one (ADR 0004) is standard MySQL, so the database cannot rank vectors for us.

## Decision

- Use **MySQL** for all data, with Flyway migrations and `BIGINT AUTO_INCREMENT` ids to match the lab's `users` table.
- Store each standard clause's 384-dimension embedding (ADR 0006's embedding model) in a **JSON column**. Moving to `VECTOR(384)` later is a new migration once every environment runs MySQL 9.0 or newer.
- Compute **cosine similarity in Java**: load the standard-clause embeddings (a few hundred), cache them in memory, and score each contract clause against them. At this size a full scan takes milliseconds, so no vector index is needed.

## Consequences

- One database for relational data and embeddings; no extra vector service to host.
- Similarity search does not scale to millions of vectors. If the clause library grows that large, a dedicated vector store becomes a new ADR.
- Postgres row-level security is not available; ownership is enforced in the service and repository layer instead (ADR 0007).
