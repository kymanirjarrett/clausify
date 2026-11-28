package com.clausify.clause;

import com.clausify.analysis.ClauseFinding;
import com.clausify.analysis.ClauseFindingRepository;
import com.clausify.analysis.RiskCategory;
import com.clausify.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** FR-9: the standard clauses most like one of the user's findings, and the library itself. */
@Service
public class ClauseSimilarityService {

    static final String NOT_FOUND = "Finding not found.";
    static final int TOP = 3;

    private final ClauseFindingRepository findings;
    private final StandardClauseRepository clauses;
    private final ClauseLibrary library;
    private final ClauseEmbedder embedder;

    public ClauseSimilarityService(ClauseFindingRepository findings, StandardClauseRepository clauses,
                                   ClauseLibrary library, ClauseEmbedder embedder) {
        this.findings = findings;
        this.clauses = clauses;
        this.library = library;
        this.embedder = embedder;
    }

    /** 404 if the finding is missing or belongs to another user. Compares only within the finding's category. */
    @Transactional(readOnly = true)
    public SimilarClausesResponse similar(Long findingId, Long userId) {
        ClauseFinding finding = findings.findOwned(findingId, userId).orElseThrow(() -> new NotFoundException(NOT_FOUND));
        float[] vector = embedder.embed(finding.getOriginalText());
        List<SimilarClausesResponse.Match> matches = library.embedded(finding.getCategory()).stream()
                .map(e -> new SimilarClausesResponse.Match(e.clause(),
                        Math.round(Math.max(0, ClauseEmbedder.cosine(vector, e.vector())) * 100) / 100.0))
                .sorted(Comparator.comparingDouble(SimilarClausesResponse.Match::similarity).reversed())
                .limit(TOP)
                .toList();
        return new SimilarClausesResponse(new SimilarClausesResponse.Finding(finding.getId(), finding.getCategory(),
                finding.getTitle(), finding.getOriginalText(), finding.getSuggestedRevision()), matches);
    }

    /** FR-9 example 2: the whole library, or one category of it. */
    @Transactional(readOnly = true)
    public List<StandardClauseResponse> library(RiskCategory category) {
        List<StandardClause> rows = category == null
                ? clauses.findAllByOrderByCategoryAscFavorableDescTitleAsc()
                : clauses.findAllByCategoryOrderByFavorableDescTitleAsc(category);
        return rows.stream().map(StandardClauseResponse::from).toList();
    }
}
