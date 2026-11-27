package com.clausify.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/auth/login}. No password rules here: a wrong password of any length is just a failed
 * login (401), counted by the per-email limit. The email cap matches the column and bounds rate-limiter keys.
 */
public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Size(max = 255, message = "Email must be 255 characters or fewer")
        String email,

        @NotBlank(message = "Password is required")
        String password) {

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=****]";
    }
}
