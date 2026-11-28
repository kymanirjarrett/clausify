package com.clausify.clause;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.CRC32;

/**
 * Word-based embeddings (ADR 0012): each word and each pair of adjacent words is hashed into one of 384 buckets.
 * Two clauses score high when they share wording, which is what "similar standard clause" mostly means for contract
 * language. It needs no model and no extra memory, and it is deterministic, so results are reproducible and testable.
 */
public class HashingEmbedder implements ClauseEmbedder {

    static final int DIMENSIONS = 384;

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        String[] words = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").strip().split(" ");
        for (int i = 0; i < words.length; i++) {
            if (words[i].isEmpty()) {
                continue;
            }
            vector[bucket(words[i])] += 1;
            if (i + 1 < words.length) {
                vector[bucket(words[i] + " " + words[i + 1])] += 0.5f;
            }
        }
        return vector;
    }

    @Override
    public String modelName() {
        return "hashing-384";
    }

    private static int bucket(String token) {
        CRC32 crc = new CRC32();
        crc.update(token.getBytes(StandardCharsets.UTF_8));
        return (int) (crc.getValue() % DIMENSIONS);
    }
}
