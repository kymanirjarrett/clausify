package com.clausify.analysis;

import com.clausify.contract.Contract;

import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * FR-6: everything the results screen shows for one analyzed contract. {@code findings} are the risky clauses,
 * highest score first; {@code favorable} lists the clauses in the user's favor; {@code categories} rolls the risky
 * findings up to the highest score per category. Never includes the contract text or embeddings.
 */
public record AnalysisResponse(
        Long contractId,
        int overallScore,
        RiskLevel riskLevel,
        String contractType,
        String summary,
        Document document,
        List<CategoryRisk> categories,
        List<Finding> findings,
        List<Finding> favorable) {

    /** Document info for the side panel; {@code analysisMs} is how long the AI analysis took. */
    public record Document(String filename, int pageCount, long sizeBytes, Instant uploadedAt, Instant analyzedAt,
                           long analysisMs) {
    }

    public record CategoryRisk(RiskCategory category, int highestScore, RiskLevel riskLevel, int findingCount) {
    }

    public record Finding(Long id, RiskCategory category, int riskScore, RiskLevel riskLevel, String title,
                          String sectionRef, String originalText, String explanation, String suggestedRevision) {

        static Finding from(ClauseFinding f) {
            return new Finding(f.getId(), f.getCategory(), f.getRiskScore(), f.getRiskLevel(), f.getTitle(),
                    f.getSectionRef(), f.getOriginalText(), f.getExplanation(), f.getSuggestedRevision());
        }
    }

    /** Highest score first; ties keep the order the findings were saved in, so the list is stable. */
    private static final Comparator<ClauseFinding> HIGHEST_FIRST =
            Comparator.comparing(ClauseFinding::getRiskScore).reversed().thenComparing(ClauseFinding::getId);

    static AnalysisResponse from(Contract contract, Analysis analysis) {
        List<ClauseFinding> risky = analysis.getFindings().stream().filter(f -> !f.isFavorable())
                .sorted(HIGHEST_FIRST).toList();
        List<ClauseFinding> inFavor = analysis.getFindings().stream().filter(ClauseFinding::isFavorable)
                .sorted(HIGHEST_FIRST).toList();
        Document document = new Document(contract.getOriginalFilename(), contract.getPageCount(),
                contract.getFileSizeBytes(), contract.getUploadedAt(), contract.getAnalyzedAt(),
                analysis.getDurationMs());
        return new AnalysisResponse(contract.getId(), analysis.getOverallScore(), analysis.getRiskLevel(),
                analysis.getContractType(), analysis.getSummary(), document, categories(risky),
                risky.stream().map(Finding::from).toList(), inFavor.stream().map(Finding::from).toList());
    }

    /** One entry per category that has a risky finding, riskiest category first. */
    private static List<CategoryRisk> categories(List<ClauseFinding> risky) {
        Map<RiskCategory, int[]> rollup = new EnumMap<>(RiskCategory.class); // {highest score, count}
        for (ClauseFinding f : risky) {
            int[] entry = rollup.computeIfAbsent(f.getCategory(), c -> new int[2]);
            entry[0] = Math.max(entry[0], f.getRiskScore());
            entry[1]++;
        }
        return rollup.entrySet().stream()
                .map(e -> new CategoryRisk(e.getKey(), e.getValue()[0], RiskLevel.fromScore(e.getValue()[0]),
                        e.getValue()[1]))
                .sorted(Comparator.comparingInt(CategoryRisk::highestScore).reversed())
                .toList();
    }
}
