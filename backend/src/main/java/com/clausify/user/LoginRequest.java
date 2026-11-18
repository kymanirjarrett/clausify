package com.clausify.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of {@code POST /api/auth/login}. No format rules here: a wrong email or password is just a failed login. */
public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Size(max = 255, message = "Email must 255 characters or less")
        String email,

        @NotBlank(message = "Password is required")
        @Size (min = 8, max = 72 ,message = "Password must be 72 characters or less")
        String password) {

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=****]";
    }
}
