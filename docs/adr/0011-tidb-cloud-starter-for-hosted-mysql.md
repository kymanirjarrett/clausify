# 11. TiDB Cloud Starter for hosted MySQL

- **Status:** Accepted
- **Date:** 2026-11-26
- **Supersedes:** [0004](0004-aiven-free-tier-for-hosted-mysql.md)

## Context

ADR 0004 put the production database on Aiven's free MySQL plan. Two things broke that decision:

- **It crash-looped production.** Aiven powers off an idle free service, and its hostname then stops resolving. From Oct 29 onward, Render's logs show `java.net.UnknownHostException: clausify-db-clausify.c.aivencloud.com` on every wake-up: Flyway cannot connect at startup, the app exits with status 1, and Render restarts it into the same failure. The Nov 10 deploys of #109, #115, and #116 all ended `update_failed`, so production kept serving the Oct 27 build.
- **Aiven is retiring free MySQL.** On Dec 21, 2026 it powers off the remaining free MySQL services and deletes their data and backups ([free plan](https://aiven.io/docs/platform/concepts/free-plan), [migration options](https://aiven.io/docs/products/mysql/howto/mysql-free-tier-migration-options)).

The budget is still $0, with no credit card.

## Decision

Use **TiDB Cloud Starter** (MySQL compatible) in AWS us-west-2 (Oregon), the same region as the Render API, with a spending limit of 0.

- Free per instance: 5 GiB row storage and 50M request units a month, no credit card while the spending limit is 0 ([spending limit](https://docs.pingcap.com/tidbcloud/manage-serverless-spend-limit/)). Its docs describe no idle power-off.
- The database starts empty: Flyway builds V1 to V6. The Aiven data (test accounts only) is not imported.
- The `clausify` database is created by hand with `CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci`, matching MySQL 8 and V4's assumptions.
- The JDBC URL uses port 4000 with `sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3`. TiDB's certificate is issued by Let's Encrypt, which the JRE trusts by default, so no custom truststore is needed ([TLS](https://docs.pingcap.com/tidbcloud/secure-connections-to-serverless-clusters/)).
- Hikari's `max-lifetime` is 5 minutes in `application-prod.yml`. TiDB's AWS public endpoint drops connections idle for 340 s and may cut any connection after 30 minutes; TiDB recommends starting at 5 minutes ([limitations](https://docs.pingcap.com/tidbcloud/serverless-limitations/)).
- CI keeps Testcontainers MySQL 8.4. No TiDB container is added.

ADR 0004 rejected TiDB because its differences "could break Flyway migrations". A smoke test on 2026-11-26 settled that: against the real instance, the existing `flyway-mysql` module applied V1 to V6 cleanly, Hibernate `validate` passed, all five foreign keys exist and are enforced (`tidb_enable_foreign_key=1`), and register, login, upload, list, and the 404 ownership check worked over HTTP. No extra Flyway module or schema change was needed.

Alternatives rejected:
- **Aiven Developer plan:** paid, so out of budget.
- **Aiven free PostgreSQL:** a database engine change for every migration and query, and it also powers off when idle.

## Consequences

- **Default collation is `utf8mb4_bin`** (case-sensitive), confirmed with `SELECT @@collation_server`. Creating the database with `utf8mb4_0900_ai_ci` handles it; a recreated database must use the same statement. `AuthService.normalizeEmail` lowercases emails anyway.
- **`AUTO_INCREMENT` IDs are unique but not guaranteed consecutive**: TiDB hands out ID ranges, so gaps can appear ([AUTO_INCREMENT](https://docs.pingcap.com/tidb/stable/auto-increment/)). Nothing in Clausify relies on consecutive IDs.
- **No `SELECT ... FOR UPDATE OF <alias>`.** Hibernate's MySQL dialect emits that form for `@Lock(PESSIMISTIC_WRITE)`, and TiDB fails with `Table 'clausify.c1_0' doesn't exist`. CI's MySQL accepts it, so tests could not catch this; the first real upload on TiDB did (#128). Row locks use a native `... FOR UPDATE` query instead (`ContractRepository.findLockedById`).
- **No stored procedures, triggers, or events** ([MySQL compatibility](https://docs.pingcap.com/tidbcloud/mysql-compatibility/)). Clausify uses none.
- **Primary keys are not enforced** (`sql_require_primary_key=0`, unlike Aiven). Every table still gets one by project rule.
- **Monthly quota:** after 50M request units, TiDB refuses new connections until the next month. Existing connections are throttled. Check usage before demos.
- **Transactions can't run longer than 30 minutes.** AnalysisService already keeps its transactions short and calls the model outside any transaction.
- **Cold start:** the smoke test from a laptop started the app in 18.4 s, with the empty-database migration taking about 14 s of that. Render's wake-up time is still dominated by Spring starting on 0.1 CPU.
