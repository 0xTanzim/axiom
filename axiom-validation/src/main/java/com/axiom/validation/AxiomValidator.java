package com.axiom.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Main entry point for Axiom validation.
 *
 * <p>Wraps Jakarta Validation (Hibernate Validator) with a simple, Axiom-friendly API.
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Define a validated record
 * record CreateUserRequest(
 *     @NotBlank String name,
 *     @Email @NotBlank String email,
 *     @Min(18) int age
 * ) {}
 *
 * // Validate and handle result
 * ValidationResult<CreateUserRequest> result = AxiomValidator.validate(request);
 * if (!result.isValid()) {
 *     ctx.status(400);
 *     ctx.json(result.errors());
 *     return;
 * }
 *
 * // Or validate and throw
 * CreateUserRequest validRequest = AxiomValidator.validateOrThrow(request);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * This class is thread-safe. The underlying ValidatorFactory is created once
 * and reused across all validation calls.
 */
public final class AxiomValidator {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();

    private AxiomValidator() {
        // Static utility class
    }

    /**
     * Validates an object against its constraint annotations.
     *
     * <p>Returns a {@link ValidationResult} that is either:
     * <ul>
     *   <li>{@link ValidationResult.Valid} — no constraint violations</li>
     *   <li>{@link ValidationResult.Invalid} — one or more violations</li>
     * </ul>
     *
     * @param <T> the object type
     * @param object the object to validate
     * @return the validation result
     * @throws NullPointerException if object is null
     */
    public static <T> ValidationResult<T> validate(T object) {
        Objects.requireNonNull(object, "object to validate must not be null");

        Validator validator = FACTORY.getValidator();
        Set<ConstraintViolation<T>> violations = validator.validate(object);

        if (violations.isEmpty()) {
            return new ValidationResult.Valid<>(object);
        }

        List<ValidationError> errors = violations.stream()
                .map(AxiomValidator::toValidationError)
                .toList();

        return new ValidationResult.Invalid<>(object, errors);
    }

    /**
     * Validates a specific property of an object.
     *
     * <p>Useful when you only want to validate one field:
     * <pre>{@code
     * ValidationResult<User> result = AxiomValidator.validateProperty(user, "email");
     * }</pre>
     *
     * @param <T> the object type
     * @param object the object containing the property
     * @param propertyName the property name to validate
     * @return the validation result
     * @throws NullPointerException if object or propertyName is null
     */
    public static <T> ValidationResult<T> validateProperty(T object, String propertyName) {
        Objects.requireNonNull(object, "object to validate must not be null");
        Objects.requireNonNull(propertyName, "propertyName must not be null");

        Validator validator = FACTORY.getValidator();
        Set<ConstraintViolation<T>> violations = validator.validateProperty(object, propertyName);

        if (violations.isEmpty()) {
            return new ValidationResult.Valid<>(object);
        }

        List<ValidationError> errors = violations.stream()
                .map(AxiomValidator::toValidationError)
                .toList();

        return new ValidationResult.Invalid<>(object, errors);
    }

    /**
     * Validates an object and returns it if valid, or throws if invalid.
     *
     * <p>Convenience method for the common pattern:
     * <pre>{@code
     * // Instead of:
     * ValidationResult<User> result = AxiomValidator.validate(user);
     * if (!result.isValid()) {
     *     throw new ValidationException(result.errors());
     * }
     * User validUser = result.value();
     *
     * // Use:
     * User validUser = AxiomValidator.validateOrThrow(user);
     * }</pre>
     *
     * @param <T> the object type
     * @param object the object to validate
     * @return the validated object (same instance)
     * @throws ValidationException if validation fails
     * @throws NullPointerException if object is null
     */
    public static <T> T validateOrThrow(T object) throws ValidationException {
        return validate(object).getOrThrow();
    }

    /**
     * Checks if an object is valid without returning detailed errors.
     *
     * <p>Useful for simple validity checks:
     * <pre>{@code
     * if (AxiomValidator.isValid(user)) {
     *     // proceed
     * }
     * }</pre>
     *
     * @param <T> the object type
     * @param object the object to check
     * @return true if valid, false if there are constraint violations
     * @throws NullPointerException if object is null
     */
    public static <T> boolean isValid(T object) {
        Objects.requireNonNull(object, "object to validate must not be null");

        Validator validator = FACTORY.getValidator();
        return validator.validate(object).isEmpty();
    }

    private static <T> ValidationError toValidationError(ConstraintViolation<T> violation) {
        String path = violation.getPropertyPath().toString();
        String message = violation.getMessage();
        Object invalidValue = violation.getInvalidValue();

        return new ValidationError(path, message, invalidValue);
    }
}
