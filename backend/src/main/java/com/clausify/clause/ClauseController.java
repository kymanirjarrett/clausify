package com.clausify.clause;

import com.clausify.analysis.RiskCategory;
import com.clausify.common.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Clauses", description = "Compare a finding with standard clauses, and browse the library (FR-9)")
@RestController
public class ClauseController {

    private final ClauseSimilarityService similarity;

    public ClauseController(ClauseSimilarityService similarity) {
        this.similarity = similarity;
    }

    /** FR-9 example 1: the 3 standard clauses most like this finding, same category. 404 if missing or not yours. */
    @Operation(summary = "Standard clauses similar to one of my findings")
    @GetMapping("/api/clauses/{findingId}/similar")
    public SimilarClausesResponse similar(@Parameter(description = "Finding id") @PathVariable @Positive Long findingId,
                                          @AuthenticationPrincipal Jwt jwt) {
        return similarity.similar(findingId, CurrentUser.id(jwt));
    }

    /** FR-9 example 2: the library, optionally one category. */
    @Operation(summary = "Browse the standard clause library", description = "Optional category filter, e.g. IP_RIGHTS.")
    @GetMapping("/api/standard-clauses")
    public List<StandardClauseResponse> library(@RequestParam(required = false) RiskCategory category) {
        return similarity.library(category);
    }
}
