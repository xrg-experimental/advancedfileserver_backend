package com.sme.afs.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validates that a file path is safe and does not allow path traversal.
 * The validator performs platform-neutral checks by normalizing separators,
 * resolving '.' and '..' segments, and rejecting absolute or escaping paths.
 */
@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, TYPE_USE })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = SafePathValidator.class)
public @interface SafePath {
    String message() default "File path is unsafe or contains traversal";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
