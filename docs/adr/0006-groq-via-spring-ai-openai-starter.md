# 6. Groq via Spring AI's OpenAI starter

- **Status:** Accepted
- **Date:** 2026-09-27

## Context

Contract analysis needs a capable LLM that returns structured JSON, at no cost during the semester. The prototype used Groq's `llama-3.3-70b-versatile`, which Groq shut down on 2026-08-16 for free and developer tiers, naming `openai/gpt-oss-120b` as a replacement (Groq deprecations page). Groq has no official Java SDK, but its API is OpenAI-compatible.

Similarity search (ADR 0003) also needs text embeddings, and Groq's free tier does not provide an embedding model.

## Decision

- Call Groq through **Spring AI's OpenAI starter** (Spring AI 2.0, which requires Spring Boot 4.0/4.1), pointed at Groq with `spring.ai.openai.base-url=https://api.groq.com/openai/v1` as the Spring AI Groq docs specify.
- Use model **`openai/gpt-oss-120b`**, read from the `GROQ_MODEL` environment variable so a future retirement is a config change, not a code change. The key comes from `GROQ_API_KEY`.
- Compute embeddings in-process with **Spring AI ONNX Transformers** and `all-MiniLM-L6-v2` (384 dimensions): free, no API key, no network call.

## Consequences

- Swapping to another OpenAI-compatible provider (or OpenAI itself) means changing the base URL, key, and model, not code.
- Free-tier rate limits apply, so analysis runs asynchronously and uploads should be rate-limited per user.
- The embedding model is loaded into the API's memory, which counts against Render's free-tier RAM.
- Groq may retire this model too; check its deprecations page when calls start failing.
