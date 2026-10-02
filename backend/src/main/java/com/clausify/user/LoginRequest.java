package com.clausify.user;

import jakarta.validation.constraints.NotBlank;

/** Body of {@code POST /api/auth/login}. No format rules here: a wrong email or password is just a failed login. */
public record LoginRequest(
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        String password) {

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=****]";
    }
}
