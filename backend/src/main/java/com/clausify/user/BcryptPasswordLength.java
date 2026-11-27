package com.clausify.user;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * At most {@link BcryptPasswordLengthValidator#MAX_BYTES} UTF-8 bytes: BCrypt's input limit counts bytes,
 * so a 40-character password of accented letters can already be too long.
 */
@Documented
@Constraint(validatedBy = BcryptPasswordLengthValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface BcryptPasswordLength {
    String message() default "Password must be 72 UTF-8 bytes or fewer";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
