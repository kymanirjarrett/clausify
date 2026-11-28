package com.clausify.eval;

import com.clausify.analysis.ContractAnalysisResult.Finding;
import com.clausify.analysis.ContractAnalyzer;
import com.clausify.analysis.RiskCategory;
import com.clausify.eval.EvalMetrics.Ratio;
import com.clausify.eval.EvalMetrics.Scores;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Loads a labeled split, runs an analyzer over it several times, and renders the docs/spec.md, Evaluation report. */
final class EvaluationRunner {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** A labeled contract with its text. */
    record Case(GoldContract gold, String text) {
    }

    /** One pass over every contract. {@code failures} counts contracts whose analysis threw. */
    record Run(Scores scores, int failures) {
    }

    private EvaluationRunner() {
    }

    /** Every {@code <id>.json} in {@code classpath:eval/<split>/}, with its {@code <id>.txt}, sorted by id. */
    static List<Case> load(String split) throws IOException {
        Resource[] labels = new PathMatchingResourcePatternResolver().getResources("classpath:eval/" + split + "/*.json");
        List<Case> cases = new ArrayList<>();
        for (Resource label : labels) {
            GoldContract gold = JSON.readValue(label.getContentAsByteArray(), GoldContract.class);
            String text = label.createRelative(gold.id() + ".txt").getContentAsString(StandardCharsets.UTF_8);
            cases.add(new Case(gold, text));
        }
        cases.sort(Comparator.comparing(c -> c.gold().id()));
        return cases;
    }

    /**
     * Analyzes every contract {@code runs} times. A failed analysis counts as no findings (every clause missed),
     * so failures lower recall instead of silently shrinking the set.
     */
    static List<Run> run(ContractAnalyzer analyzer, List<Case> cases, int runs) {
        List<Run> results = new ArrayList<>();
        for (int r = 0; r < runs; r++) {
            List<List<Finding>> findings = new ArrayList<>();
            int failures = 0;
            for (Case c : cases) {
                try {
                    findings.add(analyzer.analyze(c.text()).findings());
                } catch (RuntimeException e) {
                    failures++;
                    findings.add(List.of());
                }
            }
            results.add(new Run(EvalMetrics.score(cases.stream().map(Case::gold).toList(), findings), failures));
        }
        return results;
    }

    /** Markdown report: every metric per run, the mean, and the Wilson 95% interval over all runs pooled. */
    static String report(String split, String modelName, String promptVersion, List<Case> cases, List<Run> runs) {
        StringBuilder md = new StringBuilder();
        long goldRisky = cases.stream().flatMap(c -> c.gold().gold().stream()).filter(g -> !g.favorable()).count();
        md.append("# AI evaluation: ").append(split).append(" split, ").append(LocalDate.now()).append("\n\n")
                .append("| | |\n|---|---|\n")
                .append("| Model | `").append(modelName).append("` |\n")
                .append("| Prompt version | `").append(promptVersion).append("` |\n")
                .append("| Contracts | ").append(cases.size()).append(" |\n")
                .append("| Gold risky clauses | ").append(goldRisky).append(" |\n")
                .append("| Runs | ").append(runs.size()).append(" |\n")
                .append("| Failed analyses | ").append(runs.stream().mapToInt(Run::failures).sum()).append(" |\n")
                .append("| Labeler agreement (Cohen's kappa) | ")
                .append(format(EvalMetrics.cohensKappa(cases.stream().map(Case::gold).toList()))).append(" |\n\n");

        md.append("## Metrics\n\nTargets from docs/spec.md, Evaluation: recall at least 85%, precision at least 75%.\n\n")
                .append("| Metric | ").append(runHeaders(runs.size())).append(" Mean | Pooled 95% CI |\n")
                .append("|---|").append("---:|".repeat(runs.size())).append("---:|---|\n");
        row(md, "Recall", runs.stream().map(r -> r.scores().recall()).toList());
        row(md, "Precision", runs.stream().map(r -> r.scores().precision()).toList());
        row(md, "Level agreement (exact)", runs.stream().map(r -> r.scores().levelExact()).toList());
        row(md, "Level agreement (within one)", runs.stream().map(r -> r.scores().levelWithinOne()).toList());
        row(md, "Overall level agreement", runs.stream().map(r -> r.scores().overallLevel()).toList());

        md.append("\n## Recall by category\n\n| Category | ").append(runHeaders(runs.size())).append(" Mean | Pooled 95% CI |\n")
                .append("|---|").append("---:|".repeat(runs.size())).append("---:|---|\n");
        for (RiskCategory category : RiskCategory.values()) {
            List<Ratio> perRun = runs.stream()
                    .map(r -> r.scores().recallByCategory().getOrDefault(category, new Ratio(0, 0))).toList();
            if (perRun.stream().anyMatch(ratio -> ratio.total() > 0)) {
                row(md, category.name(), perRun);
            }
        }
        return md.toString();
    }

    private static String runHeaders(int runs) {
        StringBuilder headers = new StringBuilder();
        for (int r = 1; r <= runs; r++) {
            headers.append(" Run ").append(r).append(" |");
        }
        return headers.toString();
    }

    private static void row(StringBuilder md, String name, List<Ratio> perRun) {
        md.append("| ").append(name).append(" |");
        Ratio pooled = new Ratio(0, 0);
        double sum = 0;
        for (Ratio ratio : perRun) {
            md.append(' ').append(percent(ratio.value())).append(" (").append(ratio.hits()).append('/').append(ratio.total()).append(") |");
            pooled = pooled.plus(ratio);
            sum += ratio.total() == 0 ? 0 : ratio.value();
        }
        double[] ci = pooled.wilson95();
        md.append(' ').append(percent(sum / perRun.size())).append(" | ")
                .append(percent(ci[0])).append(" to ").append(percent(ci[1])).append(" |\n");
    }

    private static String percent(double value) {
        return Double.isNaN(value) ? "n/a" : String.format(Locale.ROOT, "%.1f%%", value * 100);
    }

    private static String format(double value) {
        return Double.isNaN(value) ? "n/a (no first-pass labels)" : String.format(Locale.ROOT, "%.2f", value);
    }

    /** Short fingerprint of the prompt files, so reports show when the prompt changed between runs. */
    static String promptVersion() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] prompts = resolver.getResources("classpath:prompts/*");
        Arrays.sort(prompts, Comparator.comparing(Resource::getFilename));
        StringBuilder all = new StringBuilder();
        for (Resource prompt : prompts) {
            all.append(prompt.getContentAsString(StandardCharsets.UTF_8));
        }
        return Integer.toHexString(all.toString().hashCode());
    }
}
