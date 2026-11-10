package com.clausify.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ContractChunkerTest {

    @Test
    void shortTextIsOneChunk() {
        assertThat(ContractChunker.split("1. Payment. Net 30.\n\n2. Term. One year.", 100))
                .containsExactly("1. Payment. Net 30.\n\n2. Term. One year.");
    }

    @Test
    void blankTextHasNoChunks() {
        assertThat(ContractChunker.split("  \n\n \n", 100)).isEmpty();
    }

    @Test
    void breaksBetweenParagraphsFirst() {
        String a = "A".repeat(60);
        String b = "B".repeat(60);

        assertThat(ContractChunker.split(a + "\n\n" + b, 100)).containsExactly(a, b);
    }

    @Test
    void longParagraphBreaksAtSentences() {
        String s1 = "x".repeat(50) + ".";
        String s2 = "y".repeat(50) + ".";

        assertThat(ContractChunker.split(s1 + " " + s2, 80)).containsExactly(s1, s2);
    }

    @Test
    void oneHugeSentenceIsCutAtTheLimit() {
        assertThat(ContractChunker.split("z".repeat(250), 100))
                .containsExactly("z".repeat(100), "z".repeat(100), "z".repeat(50));
    }

    @Test
    void everyChunkFitsAndNoTextIsLost() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            text.append("Section ").append(i).append(". The Contractor shall deliver item ").append(i)
                    .append(" on time; late delivery incurs a fee.").append(i % 3 == 0 ? "\n\n" : " ");
        }

        List<String> chunks = ContractChunker.split(text.toString(), 500);

        assertThat(chunks).allSatisfy(c -> assertThat(c.length()).isBetween(1, 500));
        assertThat(String.join("", chunks).replaceAll("\\s", ""))
                .isEqualTo(text.toString().replaceAll("\\s", ""));
    }
}
