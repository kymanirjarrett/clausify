package com.clausify.eval;

import com.clausify.analysis.ContractAnalysisResult.Finding;
import com.clausify.analysis.RiskCategory;
import com.clausify.analysis.RiskLevel;
import com.clausify.analysis.RiskScorer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** The DESIGN.md 7.9 metrics. Pure functions, so they are unit-tested without the model. */
public final class EvalMetrics {

    /** A finding matches a gold clause when its quote covers at least this share of the clause's words. */
    static final double MIN_COVERAGE = 0.5;

    /** Representative scores per gold level, so the gold labels can be run through RiskScorer like model findings. */
    static final Map<RiskLevel, Integer> GOLD_LEVEL_SCORE = Map.of(RiskLevel.LOW, 20, RiskLevel.MEDIUM, 55, RiskLevel.HIGH, 85);

    private EvalMetrics() {
    }

    /** A count out of a total, with its Wilson 95% interval. */
    public record Ratio(long hits, long total) {

        public double value() {
            return total == 0 ? Double.NaN : (double) hits / total;
        }

        /** Wilson score interval at 95% (z = 1.96): sensible even near 0%, 100%, or with few trials. */
        public double[] wilson95() {
            if (total == 0) {
                return new double[] {Double.NaN, Double.NaN};
            }
            double z = 1.96;
            double p = value();
            double n = total;
            double denominator = 1 + z * z / n;
            double centre = (p + z * z / (2 * n)) / denominator;
            double margin = z * Math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / denominator;
            return new double[] {Math.max(0, centre - margin), Math.min(1, centre + margin)};
        }

        Ratio plus(Ratio other) {
            return new Ratio(hits + other.hits, total + other.total);
        }
    }

    /** Model findings paired one to one with gold clauses. Unpaired items on either side are misses. */
    public record Matching(List<Finding> findings, List<GoldContract.GoldClause> gold, Map<Integer, Integer> goldToFinding) {
    }

    /** Everything 7.9 reports, for one run over a set of contracts. */
    public record Scores(Ratio recall, Ratio precision, Map<RiskCategory, Ratio> recallByCategory,
                         Ratio levelExact, Ratio levelWithinOne, Ratio overallLevel) {
    }

    /**
     * Pairs findings with risky gold clauses: same category, and the finding's quote covers at least half of the gold
     * clause's words or the section references match. Best coverage first; each side is used at most once.
     */
    static Matching match(List<Finding> findings, List<GoldContract.GoldClause> gold) {
        record Candidate(int goldIndex, int findingIndex, double score) {
        }
        List<Candidate> candidates = new ArrayList<>();
        for (int g = 0; g < gold.size(); g++) {
            GoldContract.GoldClause clause = gold.get(g);
            for (int f = 0; f < findings.size(); f++) {
                Finding finding = findings.get(f);
                if (finding.category() != clause.category() || finding.favorable() != clause.favorable()) {
                    continue;
                }
                double coverage = coverage(finding.originalText(), clause.text());
                boolean sameSection = sameSection(finding.sectionRef(), clause.sectionRef());
                if (coverage >= MIN_COVERAGE || sameSection) {
                    candidates.add(new Candidate(g, f, coverage + (sameSection ? 1 : 0)));
                }
            }
        }
        candidates.sort((a, b) -> Double.compare(b.score(), a.score()));
        Map<Integer, Integer> pairs = new HashMap<>();
        Set<Integer> usedFindings = new HashSet<>();
        for (Candidate c : candidates) {
            if (!pairs.containsKey(c.goldIndex()) && usedFindings.add(c.findingIndex())) {
                pairs.put(c.goldIndex(), c.findingIndex());
            }
        }
        return new Matching(findings, gold, pairs);
    }

    /** One run: each contract's findings, scored against its gold clauses. */
    static Scores score(List<GoldContract> contracts, List<List<Finding>> findingsPerContract) {
        Ratio recall = new Ratio(0, 0);
        Ratio precision = new Ratio(0, 0);
        Ratio exact = new Ratio(0, 0);
        Ratio withinOne = new Ratio(0, 0);
        Ratio overall = new Ratio(0, 0);
        Map<RiskCategory, Ratio> byCategory = new EnumMap<>(RiskCategory.class);
        RiskScorer scorer = new RiskScorer();

        for (int i = 0; i < contracts.size(); i++) {
            List<GoldContract.GoldClause> gold = contracts.get(i).gold();
            List<Finding> findings = findingsPerContract.get(i);
            Matching matching = match(findings, gold);

            for (int g = 0; g < gold.size(); g++) {
                GoldContract.GoldClause clause = gold.get(g);
                if (clause.favorable()) {
                    continue;
                }
                Integer f = matching.goldToFinding().get(g);
                Ratio one = new Ratio(f == null ? 0 : 1, 1);
                recall = recall.plus(one);
                byCategory.merge(clause.category(), one, Ratio::plus);
                if (f != null) {
                    int distance = Math.abs(RiskLevel.fromScore(findings.get(f).riskScore()).ordinal() - clause.level().ordinal());
                    exact = exact.plus(new Ratio(distance == 0 ? 1 : 0, 1));
                    withinOne = withinOne.plus(new Ratio(distance <= 1 ? 1 : 0, 1));
                }
            }
            Set<Integer> matchedFindings = new HashSet<>(matching.goldToFinding().values());
            for (int f = 0; f < findings.size(); f++) {
                if (!findings.get(f).favorable()) {
                    // Matching requires equal favorable flags, so a matched risky finding is always paired with a risky clause.
                    precision = precision.plus(new Ratio(matchedFindings.contains(f) ? 1 : 0, 1));
                }
            }
            RiskLevel modelLevel = scorer.score(findings).level();
            RiskLevel goldLevel = scorer.score(gold.stream().map(EvalMetrics::asFinding).toList()).level();
            overall = overall.plus(new Ratio(modelLevel == goldLevel ? 1 : 0, 1));
        }
        return new Scores(recall, precision, byCategory, exact, withinOne, overall);
    }

    /**
     * Cohen's kappa between the two first-pass labelers. Each clause either labeler marked (by key and category) is
     * one item; its value is the level, FAVORABLE, or NONE when that labeler skipped it. Clauses neither labeler
     * marked are not counted, so kappa here measures agreement on what was flagged, not on what was ignored.
     */
    static double cohensKappa(List<GoldContract> contracts) {
        List<String[]> items = new ArrayList<>();
        for (GoldContract contract : contracts) {
            Map<String, String> a = byUnit(contract.labelerA());
            Map<String, String> b = byUnit(contract.labelerB());
            Set<String> units = new HashSet<>(a.keySet());
            units.addAll(b.keySet());
            for (String unit : units) {
                items.add(new String[] {a.getOrDefault(unit, "NONE"), b.getOrDefault(unit, "NONE")});
            }
        }
        if (items.isEmpty()) {
            return Double.NaN;
        }
        Map<String, Integer> countA = new HashMap<>();
        Map<String, Integer> countB = new HashMap<>();
        int agree = 0;
        for (String[] item : items) {
            if (item[0].equals(item[1])) {
                agree++;
            }
            countA.merge(item[0], 1, Integer::sum);
            countB.merge(item[1], 1, Integer::sum);
        }
        double n = items.size();
        double observed = agree / n;
        double expected = 0;
        for (Map.Entry<String, Integer> entry : countA.entrySet()) {
            expected += (entry.getValue() / n) * (countB.getOrDefault(entry.getKey(), 0) / n);
        }
        return expected == 1 ? 1.0 : (observed - expected) / (1 - expected);
    }

    private static Map<String, String> byUnit(List<GoldContract.Label> labels) {
        Map<String, String> units = new HashMap<>();
        for (GoldContract.Label label : labels) {
            units.put(normalizeSection(label.key()) + "|" + label.category(),
                    label.favorable() ? "FAVORABLE" : label.level().name());
        }
        return units;
    }

    private static Finding asFinding(GoldContract.GoldClause clause) {
        return new Finding(clause.category(), GOLD_LEVEL_SCORE.get(clause.level()), clause.favorable(), "", clause.sectionRef(),
                clause.text(), "", null);
    }

    /** Share of the gold clause's distinct words that also appear in the finding's quote. */
    static double coverage(String quote, String goldText) {
        Set<String> goldWords = words(goldText);
        if (goldWords.isEmpty() || quote == null) {
            return 0;
        }
        Set<String> quoted = words(quote);
        long covered = goldWords.stream().filter(quoted::contains).count();
        return (double) covered / goldWords.size();
    }

    private static Set<String> words(String text) {
        return new HashSet<>(Arrays.asList(text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").strip().split(" ")))
                .stream().filter(w -> !w.isEmpty()).collect(java.util.stream.Collectors.toSet());
    }

    static boolean sameSection(String a, String b) {
        String x = normalizeSection(a);
        String y = normalizeSection(b);
        return !x.isEmpty() && x.equals(y);
    }

    /** "Section 7.2", "§ 7.2", and "7.2." all become "7.2"; headings are lowercased. */
    static String normalizeSection(String ref) {
        if (ref == null) {
            return "";
        }
        return ref.toLowerCase(Locale.ROOT).replaceAll("section|§|art\\.?|article", "").replaceAll("[\\s]+", " ")
                .strip().replaceAll("\\.$", "");
    }
}
