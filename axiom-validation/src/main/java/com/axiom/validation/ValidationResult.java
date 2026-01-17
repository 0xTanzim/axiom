package com.axiom.validation;

import java.util.List;
import java.util.Objects;

/**
 * Represents the result of a validation operation.
 *
 * <p>A ValidationResult is either:
 * <ul>
 *   <li>{@link Valid} — the object passed all validation constraints</li>
 *   <li>{@link Invalid} — the object has one or more validation errors</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ValidationResult<User> result = AxiomValidator.validate(user);
 *
 * if (result.isValid()) {
 *     // Safe to use result.value()
 *     processUser(result.value());
 * } else {
 *     // Handle errors
 *     result.errors().forEach(e -> log.warn("{}: {}", e.path(), e.message()));
 * }
 *
 * // Or use getOrThrow() for concise code
 * User validUser = result.getOrThrow();
 * }</pre>
 *
 * @param <T> the type of the validated object
 */
public sealed interface ValidationResult<T> permits ValidationResult.Valid, ValidationResult.Invalid {

    /**
     * Returns true if validation passed with no errors.
     *
     * @return true if valid, false if there are errors
     */
    boolean isValid();

    /**
     * Returns the validated object (even if invalid).
     *
     * @return the object that was validated
     */
    T value();

    /**
     * Returns the list of validation errors.
     *
     * @return list of errors (empty if valid)
     */
    List<ValidationError> errors();

    /**
     * Returns the validated object if valid, throws if invalid.
     *
     * @return the validated object
     * @throws ValidationException if validation failed
     */
    T getOrThrow() throws ValidationException;

    /**
     * Represents a successful validation with no errors.
     *
     * @param <T> the type of the validated object
     */
    record Valid<T>(T value) implements ValidationResult<T> {

        public Valid {
            Objects.requireNonNull(value, "value must not be null");
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public List<ValidationError> errors() {
            return List.of();
        }

        @Override
        public T getOrThrow() {
            return value;
        }
    }

    /**
     * Represents a failed validation with one or more errors.
     *
     * @param <T> the type of the validated object
     */
    record Invalid<T>(T value, List<ValidationError> errors) implements ValidationResult<T> {

        public Invalid {
            Objects.requireNonNull(value, "value must not be null");
            Objects.requireNonNull(errors, "errors must not be null");
            if (errors.isEmpty()) {
                throw new IllegalArgumentException("Invalid result must have at least one error");
            }
            errors = List.copyOf(errors);
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public T getOrThrow() throws ValidationException {
            throw new ValidationException(errors);
        }
    }
}
