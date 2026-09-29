# 1. Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-09-27

## Context

Clausify is being rebuilt from a Next.js + Supabase prototype into a Spring Boot + Angular app by a four-person team over one semester. Many decisions were made in planning chats that teammates and graders cannot see. Without a written record, the same questions get re-argued, and the reasons behind the design are lost.

## Decision

We record significant architecture decisions as ADRs in `docs/adr/`, using Michael Nygard's format (Title, Status, Context, Decision, Consequences). Records are append-only: a changed decision gets a new ADR that supersedes the old one.

## Consequences

- Anyone can learn why the system is built this way by reading a few short files.
- Writing an ADR adds a small cost to each major decision, which also forces the reasoning to be explicit.
- The repo `CLAUDE.md` links here, so AI-assisted sessions follow the same decisions.
