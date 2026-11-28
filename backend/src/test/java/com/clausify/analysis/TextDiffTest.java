package com.clausify.analysis;

import com.clausify.analysis.TextDiff.Row;
import com.clausify.analysis.TextDiff.RowType;
import com.clausify.analysis.TextDiff.Segment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextDiffTest {

    private static final String V1 = """
            1. Payment. Client shall pay each invoice within sixty (60) days of receipt.
            2. Liability. Contractor is liable for all damages without limit.
            3. Governing law. This Agreement is governed by the laws of Ohio.
            """;

    @Test
    void identicalTextIsOneUnchangedStretch() {
        TextDiff.Result result = TextDiff.compare(V1, V1);

        assertThat(result.similarity()).isEqualTo(1.0);
        assertThat(result.unrelated()).isFalse();
        assertThat(result.rows()).extracting(Row::type).containsOnly(RowType.EQUAL);
    }

    /** A PDF re-wrapped by one added word must not show every later line as changed. */
    @Test
    void differentLineWrappingIsNotAChange() {
        String rewrapped = V1.replace("each invoice within", "each invoice\nwithin").replace("liable for", "liable\n  for");

        assertThat(TextDiff.compare(V1, rewrapped).rows()).extracting(Row::type).containsOnly(RowType.EQUAL);
    }

    @Test
    void anEditedSentenceMarksOnlyTheEditedWords() {
        String v2 = V1.replace("sixty (60)", "fifteen (15)");

        List<Row> changed = TextDiff.compare(V1, v2).rows().stream().filter(r -> r.type() == RowType.CHANGED).toList();

        assertThat(changed).singleElement().satisfies(row -> {
            assertThat(row.original()).filteredOn(Segment::changed).extracting(Segment::text).containsExactly("sixty (60)");
            assertThat(row.revised()).filteredOn(Segment::changed).extracting(Segment::text).containsExactly("fifteen (15)");
            assertThat(row.revised()).filteredOn(s -> !s.changed()).extracting(Segment::text)
                    .containsExactly("Client shall pay each invoice within", "days of receipt.");
        });
    }

    @Test
    void addedAndRemovedSentencesAreTheirOwnRows() {
        String v2 = V1.replace("2. Liability. Contractor is liable for all damages without limit.\n", "")
                + "4. Non-compete. Contractor shall not work for any competitor for three years.\n";

        TextDiff.Result result = TextDiff.compare(V1, v2);

        assertThat(result.rows()).filteredOn(r -> r.type() == RowType.REMOVED).singleElement()
                .satisfies(r -> assertThat(r.original().getFirst().text()).contains("liable for all damages"));
        assertThat(result.rows()).filteredOn(r -> r.type() == RowType.ADDED).singleElement()
                .satisfies(r -> assertThat(r.revised().getFirst().text()).contains("any competitor for three years"));
    }

    /** FR-10 example 2: shared everyday words do not make two different documents look related. */
    @Test
    void unrelatedDocumentsAreFlaggedAndNotDiffed() {
        String other = """
                This lease is made between the landlord and the tenant for the premises described below.
                The tenant shall keep the premises clean and shall not keep pets without written consent.
                """;

        TextDiff.Result result = TextDiff.compare(V1, other);

        assertThat(result.unrelated()).isTrue();
        assertThat(result.similarity()).isLessThan(TextDiff.UNRELATED_BELOW);
        assertThat(result.rows()).extracting(Row::type).containsExactly(RowType.REMOVED, RowType.ADDED);
    }

    @Test
    void splitsSentencesAtEndPunctuation() {
        assertThat(TextDiff.sentences("A shall pay.  B\nshall work; C: rules apply! Why? Done"))
                .containsExactly("A shall pay.", "B shall work;", "C:", "rules apply!", "Why?", "Done");
        assertThat(TextDiff.sentences("   ")).isEmpty();
    }
}
