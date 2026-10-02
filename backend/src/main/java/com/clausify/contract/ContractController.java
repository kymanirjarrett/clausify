package com.clausify.contract;

import com.clausify.common.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
}
