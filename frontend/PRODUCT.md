# Product

<!-- impeccable:product-schema 1 -->

Visual and product context for the Angular frontend. The repository root `DESIGN.md` is the course design document (requirements, UML, architecture), not a visual design system; this folder's `DESIGN.md` (when written) is the visual one.

## Platform

web

## Users

- **Freelancers and independent contractors** (developers, designers, writers, consultants) who receive roughly 5 to 10 contracts a month and cannot justify $200 to $500 for a lawyer's review each time. Job: before signing, understand what a contract commits them to and what to push back on.
- **Small business owners** (1 to 5 employees, no legal department) doing the same first-pass review.
- Secondary: startup founders and small consulting firms needing quick first-pass reviews.

## Product Purpose

Upload a contract PDF and, in under a minute, receive an overall risk score (0 to 100, Low / Medium / High), clause-by-clause findings across eight categories with plain-language explanations and suggested revisions to negotiate with, a list of favorable clauses, comparison against standard clause language, version comparison, and a downloadable report. Success: the user signs knowing exactly what they agreed to, or negotiates better terms with concrete wording.

## Positioning

- Clausify never retains the contract file: text is extracted in memory and the PDF is discarded. Deleting a contract deletes everything derived from it.
- Every risky finding comes with specific replacement wording, not only a warning.
- Free to use (built entirely on free tiers).
- It provides information, not legal advice; every results page and report says so.

## Operating Context

- Users arrive with a contract PDF from an email or client portal, often under time pressure before a signing deadline, usually on a laptop; phones must work for checking results.
- Core loop: sign in, drop a PDF on the dashboard, wait while analysis runs (status moves Uploaded, Analyzing, Analyzed, or Failed with a retry), read findings, copy revisions into a reply to the client.
- Returning users scan their document history and revisit past results.

## Capabilities and Constraints

- Stack: Angular (standalone, zoneless) with Tailwind CSS, npm; deployed on Vercel. Backend is a Spring Boot REST API with stateless JWT auth (`Authorization: Bearer`), RFC 9457 ProblemDetail errors, and OpenAPI docs at `/swagger-ui.html`.
- Uploads: PDF only, 10 MB maximum, text-based PDFs only (scanned documents return 422 until OCR exists).
- Analysis is asynchronous; the client polls contract status.
- Eight risk categories: Payment Terms, Liability, Indemnification, IP Rights, Termination, Non-Compete, Confidentiality, Jurisdiction. Levels: Low under 40, Medium 40 to 69, High 70 and above.
- Available today (Sprint 1): register, login, current user, upload, list, contract detail and status. Analysis results, retry, delete, clause comparison, version comparison, reports, and account statistics arrive in later sprints; the UI must not present them as working before the API supports them.
- Sign in with Google is out of scope for the MVP.

## Brand Commitments

- Name: **Clausify**. No logo, palette, or typography is committed; the visual system is decided fresh. Earlier wireframes and mockups in `docs/design/` show which screens exist and what they contain, but are not a visual or interaction spec.
- Voice: **formal and precise.** Careful, exact wording; explain what a clause means and what to request in measured terms; no slang, jokes, or alarmism.

## Evidence on Hand

- No real users, testimonials, customer logos, usage statistics, or accuracy results exist yet. Do not fabricate any. Accuracy targets are planned (recall of at least 85% on a held-out evaluation set) but not yet measured.
- Synthetic sample contracts for demos: `backend/src/test/resources/contracts/`.

## Product Principles

1. Clarity over alarm: state the risk and the remedy precisely; let the score and wording carry the weight.
2. The user stays in control: Clausify informs the negotiation; it does not make the decision.
3. Privacy is visible: say plainly that files are not kept, and make deletion easy.
4. Honest status: always show where an analysis stands, including failures, and never imply a capability that does not exist yet.

## Accessibility & Inclusion

WCAG 2.2 AA: sufficient contrast, full keyboard operation (including the upload area), visible focus, screen-reader labels and live status announcements, respect for reduced motion, and risk level never conveyed by color alone (badges carry text).
