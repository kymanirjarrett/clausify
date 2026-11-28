# 12. Word-based embeddings for clause similarity

- **Status:** Accepted
- **Date:** 2026-11-27
- **Supersedes:** the embedding-model part of [0006](0006-groq-via-spring-ai-openai-starter.md). Groq for analysis is unchanged, and [0003](0003-mysql-with-java-cosine-similarity.md) (JSON vectors, cosine in Java) still applies.

## Context

ADR 0006 planned FR-9's embeddings with Spring AI ONNX Transformers and `all-MiniLM-L6-v2`, in the API's process. It noted that the model "counts against Render's free-tier RAM". Building FR-9 (#50) measured how much.

The API image ran in Docker with a hard 512 MB limit, matching Render's free tier and its `-XX:MaxRAMPercentage=70` heap. The workload was startup, embedding the 104-clause library, then repeated PDF uploads and library reads:

| Configuration | Peak resident memory |
|---|---|
| `main` without FR-9 | 424 MB |
| ONNX, full-precision model (90 MB) | 445 MB at startup, then about 490 MB (the 512 MB limit was reached) |
| ONNX, full-precision model, heap capped at 50% | The heap ran out: Spring AI reads the whole model file into a byte array |
| ONNX, 8-bit quantized model (23 MB) | 511 MB |
| ONNX, 8-bit quantized model, heap capped at 60% | 490 MB |

The ONNX runtime, the native tokenizer, and their classes add roughly 90 MB that the heap setting cannot bound. Render restarts an instance that goes over 512 MB, so a heavier PDF or a few concurrent users would risk a restart loop. The ONNX runtime jar also grows the image from 760 MB to 1.13 GB. Paying for more RAM is outside the budget.

## Decision

Embed clauses with **`HashingEmbedder`**: each word and each pair of adjacent words, lowercased, is hashed into one of 384 buckets, and similarity is the cosine of those vectors, as in ADR 0003. It is used in every environment.

- No model file, no native code, and no new dependency. Memory and image size are unchanged.
- It is deterministic, so similarity results are reproducible and covered by ordinary tests.
- `ClauseEmbedder` is the interface. A neural model can replace `HashingEmbedder` in `EmbeddingConfig` without touching the rest of FR-9. Each stored embedding records `embedding_model`, so a switch re-embeds the library automatically at startup.

## Consequences

- Similarity measures **shared wording**, not meaning. On four test queries (non-compete, IP assignment, payment, liability cap), the hashing embedder's best match was the right one each time, and the quantized ONNX model agreed on three of the four. Standard contract language reuses a lot of fixed phrasing, which is why word overlap works here. A paraphrase that shares few words with any library clause will match weakly.
- The response's `similarity` is a word-overlap score, not a semantic one; the UI shows it as a percentage, as FR-9 asks.
- **Revisit when** the API has at least 1 GB of RAM. Add an embedder that loads the 8-bit `all-MiniLM-L6-v2` (pinned by commit and checked by hash in the Docker build), then return it from `EmbeddingConfig`.
