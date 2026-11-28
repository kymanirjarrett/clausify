package com.clausify.clause;

/** Turns clause text into a vector; similar meaning gives a high cosine similarity (FR-9, ADR 0003). */
public interface ClauseEmbedder {

    float[] embed(String text);

    /** Stored with each embedding, so switching models re-embeds the library. */
    String modelName();

    /** Cosine similarity: 1 for the same direction, 0 for unrelated. Zero vectors give 0. */
    static double cosine(float[] a, float[] b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return normA == 0 || normB == 0 ? 0 : dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
