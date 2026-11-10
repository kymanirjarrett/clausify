package com.clausify.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAutoConfigurationUtil;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiCommonProperties;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;

/**
 * Checks our real {@code application.yml} and {@code application-prod.yml} against Spring AI's OpenAI
 * auto-configuration, without a database or any network call (building the client sends no request).
 * Catches misspelled or deprecated property names, which Spring would otherwise ignore silently.
 */
class GroqChatConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    OpenAiChatAutoConfiguration.class,
                    ToolCallingAutoConfiguration.class));

    @Test
    void sharedConfigCreatesNoChatModel() {
        runner.withInitializer(ctx -> load(ctx, "application.yml"))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(ChatModel.class));
    }

    @Test
    void groqModelEnvVarOverridesTheDefault() {
        // Deliberately not the default, so this fails if GROQ_MODEL were ignored.
        prodRunner().withPropertyValues("GROQ_API_KEY=test-key-not-real", "GROQ_MODEL=openai/gpt-oss-20b")
                .run(ctx -> assertThat(ctx.getBean(OpenAiChatModel.class).getOptions().getModel())
                        .isEqualTo("openai/gpt-oss-20b"));
    }

    @Test
    void prodConfigPointsTheChatModelAtGroq() {
        // GROQ_MODEL unset: the default in application-prod.yml applies.
        prodRunner().withPropertyValues("GROQ_API_KEY=test-key-not-real")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ChatModel.class);

                    // Connection settings (URL, key) go to the OpenAI SDK client, merged the same way the
                    // auto-configuration does it: chat-level values override the shared spring.ai.openai.* ones.
                    var connection = OpenAiAutoConfigurationUtil.resolveCommonProperties(
                            ctx.getBean(OpenAiCommonProperties.class), ctx.getBean(OpenAiChatProperties.class));
                    assertThat(connection.getBaseUrl()).isEqualTo("https://api.groq.com/openai/v1");
                    assertThat(connection.getApiKey()).isEqualTo("test-key-not-real");
                    // GroqContractAnalyzer does its own retries; SDK retries would multiply them.
                    assertThat(connection.getMaxRetries()).isZero();
                    assertThat(connection.getTimeout()).isEqualTo(java.time.Duration.ofSeconds(60));

                    // Request options travel with every chat call.
                    OpenAiChatModel model = ctx.getBean(OpenAiChatModel.class);
                    assertThat(model.getOptions().getModel()).isEqualTo("openai/gpt-oss-120b");
                    assertThat(model.getOptions().getReplayReasoningContent()).isFalse();
                });
    }

    private ApplicationContextRunner prodRunner() {
        return runner.withInitializer(ctx -> {
            load(ctx, "application.yml");
            load(ctx, "application-prod.yml"); // added last with addFirst, so it wins, like an active profile
        });
    }

    private static void load(ConfigurableApplicationContext ctx, String file) {
        try {
            new YamlPropertySourceLoader().load(file, new ClassPathResource(file))
                    .forEach(source -> ctx.getEnvironment().getPropertySources().addFirst(source));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file, e);
        }
    }
}
