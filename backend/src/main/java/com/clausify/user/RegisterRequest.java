package com.clausify.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/auth/register}. The 72-character password cap matches BCrypt's input limit,
 * so long passwords get a clear 400 instead of failing inside the encoder.
 */
public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be 255 characters or fewer")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 255, message = "Email must be 255 characters or fewer")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be at least 8 characters and include a letter and a number")
        @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).+$",
                message = "Password must be at least 8 characters and include a letter and a number")
        String password) {

    @Override
    public String toString() {
        return "RegisterRequest[name=" + name + ", email=" + email + ", password=****]";
    }
}
