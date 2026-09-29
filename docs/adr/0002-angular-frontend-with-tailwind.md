# 2. Angular frontend with Tailwind CSS

- **Status:** Accepted
- **Date:** 2026-09-27

## Context

The prototype was a Next.js (React) app that also held server logic (API routes calling Supabase and the LLM). In the rebuild, all business logic, security, and persistence move to the Spring Boot API, so the frontend only needs to be a client. The existing UI mockups were built with Tailwind utility classes.

## Decision

Build the frontend as an **Angular** single-page app in TypeScript, styled with **Tailwind CSS**, using npm. It is deployed separately on Vercel and talks to the API over REST with a JWT attached by an Angular `HttpInterceptor`.

- **Angular over Next.js/React:** Angular's built-in structure (dependency injection, services, typed reactive forms, routing, interceptors) mirrors the controller/service layering on the backend, gives four developers one standard way to do each thing, and needs no extra libraries for forms, HTTP, or routing. Next.js's main strengths (server rendering, server routes) are not needed once Spring Boot owns the server.
- **Tailwind over Angular Material:** the existing mockups carry over nearly as-is, and the UI keeps its own look instead of Material's default visual style.

## Consequences

- The Next.js code in `legacy/nextjs-prototype/` is reference only and is deleted once the Angular app reaches parity.
- Teammates need to learn Angular's patterns (components, services, signals or RxJS).
- Without a component library, accessible widgets (dialogs, menus) must be built carefully or taken from a headless library such as Angular CDK.
