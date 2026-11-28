package com.clausify.analysis;

import com.clausify.common.ConflictException;
import com.clausify.common.NotFoundException;
import com.clausify.contract.Contract;
import com.clausify.contract.ContractRepository;
import com.clausify.contract.ContractStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * FR-10: compares two of the user's analyzed contracts. The text diff comes from {@link TextDiff}; the risk change
 * comes from the two stored analyses, so comparing makes no AI call and costs nothing.
 */
@Service
public class ComparisonService {

    static final String NOT_FOUND = "Contract not found.";
    static final String SAME_CONTRACT = "Choose a different contract to compare with.";

    private final ContractRepository contracts;
    private final AnalysisRepository analyses;

    public ComparisonService(ContractRepository contracts, AnalysisRepository analyses) {
        this.contracts = contracts;
        this.analyses = analyses;
    }

    /** 400 if both ids are the same; 404 if either is missing or not the caller's; 409 if either is not ANALYZED. */
    @Transactional(readOnly = true)
    public ComparisonResponse compare(Long originalId, Long revisedId, Long userId) {
        if (originalId.equals(revisedId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, SAME_CONTRACT);
        }
        Contract original = owned(originalId, userId);
        Contract revised = owned(revisedId, userId);
        Analysis before = analysis(original);
        Analysis after = analysis(revised);

        TextDiff.Result diff = TextDiff.compare(original.getExtractedText(), revised.getExtractedText());
        return new ComparisonResponse(
                ComparisonResponse.Version.of(original, before),
                ComparisonResponse.Version.of(revised, after),
                after.getOverallScore() - before.getOverallScore(),
                Math.round(diff.similarity() * 100) / 100.0,
                diff.unrelated(),
                categoryChanges(before, after),
                new ComparisonResponse.Counts(count(diff, TextDiff.RowType.ADDED), count(diff, TextDiff.RowType.REMOVED),
                        count(diff, TextDiff.RowType.CHANGED)),
                diff.rows());
    }

    private Contract owned(Long id, Long userId) {
        return contracts.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }

    private Analysis analysis(Contract contract) {
        if (contract.getStatus() != ContractStatus.ANALYZED) {
            throw new ConflictException("Both contracts must be analyzed before comparing. "
                    + contract.getOriginalFilename() + " is " + contract.getStatus() + ".",
                    Map.of("contractId", contract.getId(), "contractStatus", contract.getStatus()));
        }
        return analyses.findByContractId(contract.getId()).orElseThrow(() ->
                new IllegalStateException("Contract " + contract.getId() + " is ANALYZED without an analysis"));
    }

    /** Categories whose highest risky score moved, riskiest change first. Favorable findings are not risk. */
    static List<ComparisonResponse.CategoryChange> categoryChanges(Analysis before, Analysis after) {
        Map<RiskCategory, Integer> old = highestByCategory(before);
        Map<RiskCategory, Integer> now = highestByCategory(after);
        List<ComparisonResponse.CategoryChange> changes = new ArrayList<>();
        for (RiskCategory category : RiskCategory.values()) {
            Integer a = old.get(category);
            Integer b = now.get(category);
            if (!Objects.equals(a, b)) {
                changes.add(new ComparisonResponse.CategoryChange(category, a, b));
            }
        }
        changes.sort((x, y) -> Integer.compare(magnitude(y), magnitude(x)));
        return changes;
    }

    private static int magnitude(ComparisonResponse.CategoryChange change) {
        return Math.abs((change.after() == null ? 0 : change.after()) - (change.before() == null ? 0 : change.before()));
    }

    private static Map<RiskCategory, Integer> highestByCategory(Analysis analysis) {
        Map<RiskCategory, Integer> highest = new EnumMap<>(RiskCategory.class);
        for (ClauseFinding finding : analysis.getFindings()) {
            if (!finding.isFavorable()) {
                highest.merge(finding.getCategory(), finding.getRiskScore(), Math::max);
            }
        }
        return highest;
    }

    private static long count(TextDiff.Result diff, TextDiff.RowType type) {
        return diff.rows().stream().filter(row -> row.type() == type).count();
    }
}
