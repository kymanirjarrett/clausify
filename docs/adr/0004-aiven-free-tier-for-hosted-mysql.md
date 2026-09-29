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
- **TODO:** when the service is created, record its MySQL version and exact storage limit here (Aiven's docs list up to 8 GB of disk), and pin the Docker image in `docker-compose.yml` to the same MySQL version. Until then, Docker stays on 8.4.
