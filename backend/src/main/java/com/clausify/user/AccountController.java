package com.clausify.user;

import com.clausify.common.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Account", description = "The signed-in user's profile and statistics (FR-12)")
@RestController
@RequestMapping("/api/users/me")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(summary = "Get my account", description = "Name, email, and phone number.")
    @GetMapping
    public AccountResponse get(@AuthenticationPrincipal Jwt jwt) {
        return accountService.get(CurrentUser.id(jwt));
    }

    /** FR-12 example 2: change name and phone; fields left out stay as they are. */
    @Operation(summary = "Update my account", description = "Send only the fields to change. An empty phoneNumber clears it.")
    @PatchMapping
    public AccountResponse update(@Valid @RequestBody UpdateAccountRequest request, @AuthenticationPrincipal Jwt jwt) {
        return accountService.update(CurrentUser.id(jwt), request);
    }

    /** FR-12 example 1: contracts uploaded and analyzed, and the average risk score. */
    @Operation(summary = "Get my statistics")
    @GetMapping("/stats")
    public AccountStatsResponse stats(@AuthenticationPrincipal Jwt jwt) {
        return accountService.stats(CurrentUser.id(jwt));
    }
}
