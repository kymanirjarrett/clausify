# 5. Extract text, do not store PDFs

- **Status:** Accepted
- **Date:** 2026-09-27

## Context

Users upload contract PDFs up to 10 MB. Everything after the upload (analysis, retry, comparison, reports) only needs the text. Every free place to keep the files had a real drawback:

| Option | Problem |
|---|---|
| Supabase Storage | Free projects pause after a week of inactivity |
| Cloudflare R2 | Requires a credit card |
| Render disk | Not persistent on the free tier; wiped on redeploy |
| MySQL `BLOB` | PDFs would quickly fill Aiven's small free storage |

## Decision

**Never persist the PDF.** `POST /api/contracts` reads the upload in memory, extracts text with Apache PDFBox in the same request, and discards the bytes.

1. PDF only, max 10 MB.
2. If no usable text comes out (scanned or corrupt file), return `422 Unprocessable Entity` with a clear message. OCR is a possible future enhancement.
3. Otherwise save a `contracts` row with the extracted text and status `UPLOADED`, return `202 Accepted`, and start `@Async` analysis: `UPLOADED → ANALYZING → ANALYZED / FAILED`.
4. Retry, comparison, and report generation all work from the stored text.

## Consequences

- Text is roughly 1% of the PDF's size, so storage stays small.
- Privacy is a feature: "we never retain your contract file," which matters for a legal product.
- Users cannot download their original file from Clausify, and scanned PDFs are rejected until OCR exists.
- We do not build a `FileStorageService` abstraction now, since nothing would implement it. If file retention is needed later, add that interface with an object-storage implementation in a new ADR.
