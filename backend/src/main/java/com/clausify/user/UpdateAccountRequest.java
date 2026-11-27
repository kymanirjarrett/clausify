package com.clausify.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PATCH /api/users/me}. A missing field is left unchanged; an empty phone number clears it.
 * Email is not editable here because it is the sign-in identity.
 */
public record UpdateAccountRequest(
        @Size(max = 255, message = "Name must be 255 characters or fewer")
        @Pattern(regexp = ".*\\S.*", message = "Name cannot be blank")
        String name,

        // Matches the VARCHAR(15) column from V3: digits with optional +, spaces, dots, dashes, parentheses.
        @Size(max = 15, message = "Phone number must be 15 characters or fewer")
        @Pattern(regexp = "^$|^\\+?[0-9 ().-]{7,15}$", message = "Phone number may contain only digits, spaces, and + ( ) . -")
        String phoneNumber) {
}
