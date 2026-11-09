package com.clausify.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.clausify.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

/**
 * Guards the "local and tests never call the AI" rule (CLAUDE.md): outside {@code prod} the only analyzer is
 * the fake one and no Spring AI chat client exists, so nothing can reach Groq even by accident.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ContractAnalyzerProfileTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ContractAnalyzer analyzer;

    @Test
    void usesTheFakeAnalyzerOutsideProd() {
        assertThat(analyzer).isInstanceOf(FakeContractAnalyzer.class);
        assertThat(analyzer.modelName()).isEqualTo(FakeContractAnalyzer.MODEL_NAME);
    }

    @Test
    void createsNoChatModelOutsideProd() {
        assertThat(context.getBeanNamesForType(ChatModel.class)).isEmpty();
    }

    @Test
    void fakeFindingsAreValidForTheSchema() {
        ContractAnalysisResult result = analyzer.analyze("any text");

        assertThat(result.findings()).isNotEmpty().allSatisfy(f -> {
            assertThat(f.category()).isNotNull();
            assertThat(f.riskScore()).isBetween(0, 100);
            assertThat(f.title()).isNotBlank().hasSizeLessThanOrEqualTo(255);
            assertThat(f.originalText()).isNotBlank();
            assertThat(f.explanation()).isNotBlank();
        });
    }
}
