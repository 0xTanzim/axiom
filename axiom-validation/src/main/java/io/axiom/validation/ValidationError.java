package io.axiom.validation;

import java.util.Objects;

/**
 * Represents a single validation error with field path, message, and invalid value.
 *
 * <p>Example:
 * <pre>{@code
 * ValidationError error = new ValidationError(
 *     "user.email",
 *     "must be a valid email address",
 *     "invalid-email"
 * );
 *
 * // Use in error response
 * ctx.json(Map.of(
 *     "field", error.path(),
 *     "message", error.message()
 * ));
 * }</pre>
 *
 * @param path the property path (e.g., "email", "address.city", "items[0].name")
 * @param message the human-readable error message
 * @param invalidValue the actual value that failed validation (may be null)
 */
public record ValidationError(
        String path,
        String message,
        Object invalidValue
) {

    /**
     * Creates a validation error.
     *
     * @param path the property path
     * @param message the error message
     * @param invalidValue the invalid value (may be null)
     */
    public ValidationError {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }

    /**
     * Creates a validation error without the invalid value.
     *
     * @param path the property path
     * @param message the error message
     */
    public ValidationError(String path, String message) {
        this(path, message, null);
    }

    @Override
    public String toString() {
        if (invalidValue == null) {
            return "%s: %s".formatted(path, message);
        }
        return "%s: %s (was: %s)".formatted(path, message, invalidValue);
    }
}
