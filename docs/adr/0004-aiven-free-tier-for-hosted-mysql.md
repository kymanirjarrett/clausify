# 4. Aiven free tier for hosted MySQL

- **Status:** Accepted
- **Date:** 2026-09-27

## Context

The deployed API (on Render) needs a hosted MySQL database that costs nothing, needs no credit card, and lasts the whole semester. Render's own free tier has no MySQL and no persistent disk.

## Decision

Use **Aiven for MySQL on the free plan**. Per Aiven's docs it has no time limit and no credit card requirement, with 1 CPU, 1 GB RAM, backups, and a `max_connections` limit of 20. The API connects with a full JDBC URL (`DB_URL`) copied from the Aiven console, including its TLS settings, so nothing about the connection is guessed.

Alternatives considered:
- **TiDB Cloud Starter:** MySQL-compatible but not MySQL; differences in SQL and DDL behaviour could break Flyway migrations or diverge from what the course grades.
- **Self-hosting on Render:** no persistent disk on the free tier, so data would be lost on redeploy.

## Consequences

- Aiven powers off free services after a period of inactivity, emailing a warning first; someone must power it back on in the console. **Wake it before every demo or grading session.**
- Free services have no choice of region, no VPC, no static IP, and no connection pooling. The API's connection pool must stay well under 20 connections.
- **Created 2026-10-27:** `clausify-db`, MySQL **8.4.11**, in DigitalOcean `sfo`; the Render API runs in Oregon, the nearest Render region. Local Docker and Testcontainers use the `mysql:8.4` tag, the same release line, so no change was needed.
- Aiven enables `sql_require_primary_key`: every new table needs a primary key, or its migration fails in production.
- The API's connection pool is Spring Boot's default (HikariCP, 10 connections), well under the 20-connection limit.
