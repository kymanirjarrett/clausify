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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analysis", description = "Read a contract's AI risk analysis (FR-6)")
@RestController
public class AnalysisController {

    private final AnalysisResultService analysisResultService;

    public AnalysisController(AnalysisResultService analysisResultService) {
        this.analysisResultService = analysisResultService;
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
}
