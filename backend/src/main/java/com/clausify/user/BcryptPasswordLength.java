package com.clausify.user;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = BcryptPasswordLengthValidator.class)
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, TYPE_USE})
@Retention(RUNTIME)
public @interface BcryptPasswordLength {
    String message() default "Password must be 72 UTF-8 bytes or fewer";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
