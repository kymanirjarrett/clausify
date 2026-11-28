package com.clausify.analysis;

import com.clausify.common.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analysis", description = "Read a contract's AI risk analysis (FR-6) and compare two versions (FR-10)")
@RestController
public class AnalysisController {

    private final AnalysisResultService analysisResultService;
    private final ComparisonService comparisonService;

    public AnalysisController(AnalysisResultService analysisResultService, ComparisonService comparisonService) {
        this.analysisResultService = analysisResultService;
        this.comparisonService = comparisonService;
    }

    /**
     * FR-6: score, risk by category, document info, and findings, riskiest first.
     * 404 if missing or not yours; 409 with {@code contractStatus} while it is not ANALYZED yet.
     */
    @Operation(summary = "Get a contract's analysis",
            description = "Only for a contract with status ANALYZED. While UPLOADED or ANALYZING, poll GET /api/contracts/{id}.")
    @GetMapping("/api/contracts/{id}/analysis")
    public AnalysisResponse get(@Parameter(description = "Contract id") @PathVariable @Positive Long id,
                                @AuthenticationPrincipal Jwt jwt) {
        return analysisResultService.get(id, CurrentUser.id(jwt));
    }

    /**
     * FR-10: compares the original ({@code id}) with a revised version ({@code otherId}). Both must be yours and
     * ANALYZED. Makes no AI call. 400 if the ids match, 404 if either is missing or not yours, 409 if either is not
     * analyzed yet.
     */
    @Operation(summary = "Compare two versions of a contract",
            description = "Text diff by sentence and word, the overall risk change, and per-category risk changes.")
    @PostMapping("/api/contracts/{id}/compare/{otherId}")
    public ComparisonResponse compare(@Parameter(description = "Original contract id") @PathVariable @Positive Long id,
                                      @Parameter(description = "Revised contract id") @PathVariable @Positive Long otherId,
                                      @AuthenticationPrincipal Jwt jwt) {
        return comparisonService.compare(id, otherId, CurrentUser.id(jwt));
    }
}
