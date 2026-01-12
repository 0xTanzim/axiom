package io.axiom.core.json;

import io.axiom.core.error.*;

/**
 * Exception thrown when JSON serialization or deserialization fails.
 *
 * @since 0.1.0
 */
public final class JsonException extends AxiomException {

    private final Class<?> targetType;

    /**
     * Creates a JSON exception for serialization failure.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public JsonException(final String message, final Throwable cause) {
        super(message, cause);
        this.targetType = null;
    }

    /**
     * Creates a JSON exception for deserialization failure.
     *
     * @param message    the error message
     * @param targetType the type being deserialized to
     * @param cause      the underlying cause
     */
    public JsonException(final String message, final Class<?> targetType, final Throwable cause) {
        super(message, cause);
        this.targetType = targetType;
    }

    /**
     * Returns the target type if this was a deserialization failure.
     *
     * @return the target type, or null for serialization failures
     */
    public Class<?> targetType() {
        return this.targetType;
    }
}
