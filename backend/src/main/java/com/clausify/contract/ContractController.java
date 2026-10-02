package com.clausify.contract;

import com.clausify.common.CurrentUser;
import com.clausify.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@Tag(name = "Contracts", description = "Upload and manage contracts (FR-3, FR-4, FR-7, FR-8)")
@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    /**
     * FR-3: 202 Accepted, because analysis continues in the background; poll the Location URL for status.
     * 413 over 10 MB, 415 if not a PDF, 422 if no text can be read.
     */
    @Operation(summary = "Upload a contract PDF",
            description = "PDF only, up to 10 MB. Text is extracted immediately and the file is discarded; analysis runs in the background.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContractSummaryResponse> upload(@RequestPart("file") MultipartFile file,
                                                          @AuthenticationPrincipal Jwt jwt) {
        ContractSummaryResponse contract = contractService.upload(file, CurrentUser.id(jwt));
        return ResponseEntity.accepted().location(URI.create("/api/contracts/" + contract.id())).body(contract);
    }

    /** FR-4: the caller's contracts, newest first. {@code page} is zero-based; {@code size} is at most 100. */
    @Operation(summary = "List my contracts", description = "Newest first. Query parameters: page (0-based), size (default 20, max 100).")
    @GetMapping
    public PageResponse<ContractSummaryResponse> list(@ParameterObject @PageableDefault(size = 20) Pageable pageable,
                                                      @AuthenticationPrincipal Jwt jwt) {
        return contractService.list(CurrentUser.id(jwt), pageable);
    }

    /** FR-4: status and details; poll while UPLOADED or ANALYZING. 404 if missing or not yours. */
    @Operation(summary = "Get one of my contracts")
    @GetMapping("/{id}")
    public ContractDetailResponse get(@Parameter(description = "Contract id") @PathVariable Long id,
                                      @AuthenticationPrincipal Jwt jwt) {
        return contractService.get(id, CurrentUser.id(jwt));
    }
}
