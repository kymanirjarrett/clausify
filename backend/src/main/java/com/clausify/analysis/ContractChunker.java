package com.clausify.analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits contract text into chunks of at most {@code maxChars} characters so each one fits a single model request.
 * It breaks at paragraph boundaries first, then sentences, and only cuts mid-sentence when one sentence alone is
 * longer than a chunk. Every character of the input (other than whitespace at a break) ends up in exactly one chunk.
 */
final class ContractChunker {

    private static final String PARAGRAPH_BREAK = "\\n\\s*\\n";
    private static final String SENTENCE_BREAK = "(?<=[.!?;:])\\s+";

    private ContractChunker() {
    }

    static List<String> split(String text, int maxChars) {
        if (maxChars < 1) {
            throw new IllegalArgumentException("maxChars must be positive");
        }
        List<Piece> pieces = new ArrayList<>();
        for (String paragraph : text.strip().split(PARAGRAPH_BREAK)) {
            String p = paragraph.strip();
            if (p.isEmpty()) {
                continue;
            }
            if (p.length() <= maxChars) {
                pieces.add(new Piece(p, true));
                continue;
            }
            boolean first = true;
            for (String sentence : p.split(SENTENCE_BREAK)) {
                for (int start = 0; start < sentence.length(); start += maxChars) {
                    pieces.add(new Piece(sentence.substring(start, Math.min(sentence.length(), start + maxChars)), first));
                    first = false;
                }
            }
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (Piece piece : pieces) {
            String separator = current.isEmpty() ? "" : (piece.startsParagraph() ? "\n\n" : " ");
            if (current.length() + separator.length() + piece.text().length() > maxChars) {
                chunks.add(current.toString());
                current.setLength(0);
                separator = "";
            }
            current.append(separator).append(piece.text());
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    private record Piece(String text, boolean startsParagraph) {
    }
}
