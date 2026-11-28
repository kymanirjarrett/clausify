package com.clausify.eval;

import com.clausify.analysis.GroqAnalyzerProperties;
import com.clausify.analysis.GroqContractAnalyzer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * DESIGN.md 7.9: runs the real GroqContractAnalyzer over a labeled split and writes a report to docs/evaluation/.
 * Tagged "eval" and excluded from the normal build, because it calls Groq and uses the free-tier quota.
 *
 * <pre>
 * GROQ_API_KEY=... ./mvnw test -Dsurefire.excludedGroups= -Dgroups=eval -Deval.split=dev -Deval.runs=3
 * </pre>
 */
@Tag("eval")
class AiEvaluationTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(GroqAnalyzerProperties.class)
    static class Props {
    }

    @Test
    void evaluateRealModel() throws Exception {
        assumeTrue(System.getenv("GROQ_API_KEY") != null, "Set GROQ_API_KEY to run the evaluation");
        String split = System.getProperty("eval.split", "test");
        int runs = Integer.getInteger("eval.runs", 3);
        List<EvaluationRunner.Case> cases = EvaluationRunner.load(split);
        assumeTrue(!cases.isEmpty(), "No labeled contracts in src/test/resources/eval/" + split);

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OpenAiChatAutoConfiguration.class, ToolCallingAutoConfiguration.class))
                .withUserConfiguration(Props.class)
                .withInitializer(ctx -> {
                    var sources = ctx.getEnvironment().getPropertySources();
                    for (String file : List.of("application.yml", "application-prod.yml")) {
                        try {
                            new YamlPropertySourceLoader().load(file, new ClassPathResource(file)).forEach(sources::addFirst);
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                })
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    GroqContractAnalyzer analyzer = new GroqContractAnalyzer(ctx.getBean(OpenAiChatModel.class),
                            ctx.getBean(GroqAnalyzerProperties.class));
                    List<EvaluationRunner.Run> results = EvaluationRunner.run(analyzer, cases, runs);
                    String report = EvaluationRunner.report(split, analyzer.modelName(), EvaluationRunner.promptVersion(),
                            cases, results);
                    Path out = Path.of("..", "docs", "evaluation",
                            LocalDate.now() + "-" + split + "-" + analyzer.modelName().replaceAll("[^A-Za-z0-9.-]", "_") + ".md");
                    Files.writeString(out, report);
                    System.out.println("Evaluation report written to " + out.toAbsolutePath().normalize());
                });
    }
}
