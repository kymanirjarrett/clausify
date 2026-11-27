package com.clausify.user;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

/** Null passes here; {@code @NotBlank} reports a missing password. */
public class BcryptPasswordLengthValidator implements ConstraintValidator<BcryptPasswordLength, String> {

    static final int MAX_BYTES = 72;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }
}
