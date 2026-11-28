package com.clausify.analysis;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FR-10: what changed between two versions of a contract's text.
 * <p>
 * PDF text wraps differently whenever a word is added, so lines are useless units. The text is split into sentences,
 * diffed sentence by sentence, and then each changed block is diffed word by word so only the edited words are
 * marked. Before any of that, a cheap word-pair overlap decides whether the documents are related at all; unrelated
 * documents skip the diff, which would be both meaningless and the slowest case for the algorithm.
 */
final class TextDiff {

    /** Below this share of shared word pairs, two texts are treated as different documents (FR-10 example 2). */
    static final double UNRELATED_BELOW = 0.25;
    /** Word-level detail only for changed blocks up to this size; larger blocks are marked changed as a whole. */
    static final int MAX_WORDS_FOR_DETAIL = 2000;

    enum RowType { EQUAL, CHANGED, ADDED, REMOVED }

    record Segment(String text, boolean changed) {
    }

    /** One stretch of the documents, as the original and the revised version show it. */
    record Row(RowType type, List<Segment> original, List<Segment> revised) {
    }

    record Result(double similarity, boolean unrelated, List<Row> rows) {
    }

    private TextDiff() {
    }

    static Result compare(String original, String revised) {
        List<String> oldSentences = sentences(original);
        List<String> newSentences = sentences(revised);
        double similarity = similarity(words(original), words(revised));
        if (similarity < UNRELATED_BELOW) {
            List<Row> rows = new ArrayList<>();
            if (!oldSentences.isEmpty()) {
                rows.add(new Row(RowType.REMOVED, List.of(new Segment(String.join(" ", oldSentences), true)), List.of()));
            }
            if (!newSentences.isEmpty()) {
                rows.add(new Row(RowType.ADDED, List.of(), List.of(new Segment(String.join(" ", newSentences), true))));
            }
            return new Result(similarity, true, rows);
        }

        List<Row> rows = new ArrayList<>();
        for (AbstractDelta<String> delta : DiffUtils.diff(oldSentences, newSentences, true).getDeltas()) {
            String before = String.join(" ", delta.getSource().getLines());
            String after = String.join(" ", delta.getTarget().getLines());
            switch (delta.getType()) {
                case EQUAL -> rows.add(new Row(RowType.EQUAL, List.of(new Segment(before, false)),
                        List.of(new Segment(after, false))));
                case DELETE -> rows.add(new Row(RowType.REMOVED, List.of(new Segment(before, true)), List.of()));
                case INSERT -> rows.add(new Row(RowType.ADDED, List.of(), List.of(new Segment(after, true))));
                case CHANGE -> rows.add(changed(before, after));
            }
        }
        return new Result(similarity, false, rows);
    }

    /** A changed block, with only the edited words marked on each side. */
    private static Row changed(String before, String after) {
        List<String> oldWords = words(before);
        List<String> newWords = words(after);
        if (oldWords.size() > MAX_WORDS_FOR_DETAIL || newWords.size() > MAX_WORDS_FOR_DETAIL) {
            return new Row(RowType.CHANGED, List.of(new Segment(before, true)), List.of(new Segment(after, true)));
        }
        List<Segment> original = new ArrayList<>();
        List<Segment> revised = new ArrayList<>();
        for (AbstractDelta<String> delta : DiffUtils.diff(oldWords, newWords, true).getDeltas()) {
            boolean equal = delta.getType() == com.github.difflib.patch.DeltaType.EQUAL;
            append(original, delta.getSource().getLines(), !equal);
            append(revised, delta.getTarget().getLines(), !equal);
        }
        return new Row(RowType.CHANGED, original, revised);
    }

    /** Adds words as a segment, merging with the previous one when it has the same changed flag. */
    private static void append(List<Segment> segments, List<String> words, boolean changed) {
        if (words.isEmpty()) {
            return;
        }
        String text = String.join(" ", words);
        if (!segments.isEmpty() && segments.getLast().changed() == changed) {
            Segment last = segments.removeLast();
            segments.add(new Segment(last.text() + " " + text, changed));
        } else {
            segments.add(new Segment(text, changed));
        }
    }

    /** Sentences, with PDF line breaks and repeated spaces collapsed. A sentence ends at . ; : ! or ?. */
    static List<String> sentences(String text) {
        String flat = text.replaceAll("\\s+", " ").strip();
        return flat.isEmpty() ? List.of() : Arrays.asList(flat.split("(?<=[.;:!?])\\s+"));
    }

    private static List<String> words(String text) {
        String flat = text.replaceAll("\\s+", " ").strip();
        return flat.isEmpty() ? List.of() : Arrays.asList(flat.split(" "));
    }

    /**
     * Dice coefficient over adjacent word pairs, ignoring case: 1.0 for the same text, near 0 for unrelated ones.
     * Pairs rather than single words, because any two contracts share words like "the" and "shall".
     */
    static double similarity(List<String> a, List<String> b) {
        Map<String, Integer> pairsA = pairs(a);
        Map<String, Integer> pairsB = pairs(b);
        int totalA = pairsA.values().stream().mapToInt(Integer::intValue).sum();
        int totalB = pairsB.values().stream().mapToInt(Integer::intValue).sum();
        if (totalA + totalB == 0) {
            return 1.0;
        }
        int shared = 0;
        for (Map.Entry<String, Integer> entry : pairsA.entrySet()) {
            shared += Math.min(entry.getValue(), pairsB.getOrDefault(entry.getKey(), 0));
        }
        return 2.0 * shared / (totalA + totalB);
    }

    private static Map<String, Integer> pairs(List<String> words) {
        Map<String, Integer> pairs = new HashMap<>();
        for (int i = 0; i + 1 < words.size(); i++) {
            pairs.merge(words.get(i).toLowerCase() + " " + words.get(i + 1).toLowerCase(), 1, Integer::sum);
        }
        return pairs;
    }
}
