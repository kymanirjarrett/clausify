package com.clausify.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ModelOutputParserTest {

    private static final String FINDING = """
            {"category":"LIABILITY","riskScore":85,"favorable":false,"title":"Unlimited liability",
             "sectionRef":"7.2","originalText":"Contractor is liable for all damages.",
             "explanation":"No cap.","suggestedRevision":"Cap at fees paid."}""";

    @Test
    void parsesAValidReply() throws Exception {
        var result = ModelOutputParser.parse(
                "{\"contractType\":\"Consulting Agreement\",\"summary\":\"Risky.\",\"findings\":[" + FINDING + "]}", 15);

        assertThat(result.contractType()).isEqualTo("Consulting Agreement");
        assertThat(result.findings()).singleElement().satisfies(f -> {
            assertThat(f.category()).isEqualTo(RiskCategory.LIABILITY);
            assertThat(f.riskScore()).isEqualTo(85);
            assertThat(f.favorable()).isFalse();
            assertThat(f.sectionRef()).isEqualTo("7.2");
        });
    }

    @Test
    void emptyFindingsAreValid() throws Exception {
        // A contract with nothing worth flagging is a legitimate result (overall score 0).
        var result = ModelOutputParser.parse("{\"contractType\":null,\"summary\":null,\"findings\":[]}", 15);

        assertThat(result.findings()).isEmpty();
        assertThat(result.contractType()).isNull();
    }

    @Test
    void favorableFindingMayOmitARevision() throws Exception {
        String favorable = FINDING.replace("\"favorable\":false", "\"favorable\":true")
                .replace("\"Cap at fees paid.\"", "null");

        var result = ModelOutputParser.parse("{\"contractType\":null,\"summary\":null,\"findings\":[" + favorable + "]}", 15);

        assertThat(result.findings().getFirst().suggestedRevision()).isNull();
    }

    @ParameterizedTest(name = "rejects {0}")
    @ValueSource(strings = {
            "",
            "not json",
            "[1, 2]",
            "{\"contractType\":null,\"summary\":null,\"findings\":[",                       // truncated
            "{\"contractType\":null,\"summary\":null}",                                    // no findings
            "{\"contractType\":null,\"summary\":null,\"findings\":{}}",                    // findings not an array
            "{\"contractType\":null,\"summary\":null,\"findings\":[],\"extra\":1}",        // unexpected field
            "{\"contractType\":7,\"summary\":null,\"findings\":[]}",                        // wrong type
    })
    void rejectsMalformedReplies(String reply) {
        assertThatThrownBy(() -> ModelOutputParser.parse(reply, 15)).isInstanceOf(InvalidModelOutputException.class);
    }

    @ParameterizedTest(name = "rejects finding with {0}")
    @ValueSource(strings = {
            "\"category\":\"LIABILITY\"|\"category\":\"WARRANTY\"",        // unknown category
            "\"category\":\"LIABILITY\"|\"category\":\"liability\"",       // no case folding
            "\"riskScore\":85|\"riskScore\":101",
            "\"riskScore\":85|\"riskScore\":-1",
            "\"riskScore\":85|\"riskScore\":85.5",
            "\"riskScore\":85|\"riskScore\":\"85\"",
            "\"favorable\":false|\"favorable\":\"false\"",
            "\"title\":\"Unlimited liability\"|\"title\":\"  \"",
            "\"originalText\":\"Contractor is liable for all damages.\"|\"originalText\":null",
            "\"explanation\":\"No cap.\"|\"explanation\":\"\"",
            "\"sectionRef\":\"7.2\"|\"sectionRef\":\"7.2\",\"injected\":true",
    })
    void rejectsInvalidFindings(String replacement) {
        String[] parts = replacement.split("\\|", 2);
        String bad = FINDING.replace(parts[0], parts[1]);

        assertThatThrownBy(() -> ModelOutputParser.parse("{\"contractType\":null,\"summary\":null,\"findings\":[" + bad + "]}", 15))
                .isInstanceOf(InvalidModelOutputException.class);
    }

    @Test
    void rejectsFieldsLongerThanTheirColumns() {
        String longTitle = FINDING.replace("Unlimited liability", "t".repeat(ModelOutputParser.MAX_TITLE + 1));
        String exactTitle = FINDING.replace("Unlimited liability", "t".repeat(ModelOutputParser.MAX_TITLE));

        assertThatThrownBy(() -> ModelOutputParser.parse(wrap(longTitle), 15))
                .isInstanceOf(InvalidModelOutputException.class);
        org.assertj.core.api.Assertions.assertThatCode(() -> ModelOutputParser.parse(wrap(exactTitle), 15))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMoreFindingsThanAllowed() {
        assertThatThrownBy(() -> ModelOutputParser.parse(
                "{\"contractType\":null,\"summary\":null,\"findings\":[" + FINDING + "," + FINDING + "]}", 1))
                .isInstanceOf(InvalidModelOutputException.class);
    }

    @Test
    void rejectionMessagesNeverContainTheModelOutput() {
        String secret = FINDING.replace("\"WARRANTY\"", "").replace("\"LIABILITY\"", "\"SECRET-CONTRACT-TEXT\"");

        assertThatThrownBy(() -> ModelOutputParser.parse(wrap(secret), 15))
                .hasMessageNotContaining("SECRET-CONTRACT-TEXT");
    }

    private static String wrap(String finding) {
        return "{\"contractType\":null,\"summary\":null,\"findings\":[" + finding + "]}";
    }
}
