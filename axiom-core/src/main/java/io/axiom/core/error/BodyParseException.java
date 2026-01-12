package io.axiom.core.error;

/**
 * Exception thrown when request body parsing fails.
 *
 * <p>
 * Corresponds to HTTP 400 Bad Request.
 *
 * <h2>Common Causes</h2>
 * <ul>
 * <li>Invalid JSON syntax</li>
 * <li>Type mismatch (e.g., string where number expected)</li>
 * <li>Missing required fields</li>
 * <li>Unsupported content type</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * 
 * <pre>{@code
 * app.onError((c, e) -> {
 *     if (e instanceof BodyParseException parseError) {
 *         c.status(400);
 *         c.json(Map.of(
 *                 "error", "Invalid Request Body",
 *                 "details", parseError.getMessage()));
 *     }
 * });
 * }</pre>
 *
 * @since 0.1.0
 */
public final class BodyParseException extends AxiomException {

    private final Class<?> targetType;

    /**
     * Creates a body parse exception.
     *
     * @param message    error description
     * @param targetType the type that was being parsed to
     */
    public BodyParseException(String message, Class<?> targetType) {
        super(message);
        this.targetType = targetType;
    }

    /**
     * Creates a body parse exception with cause.
     *
     * @param message    error description
     * @param targetType the type that was being parsed to
     * @param cause      the underlying parsing exception
     */
    public BodyParseException(String message, Class<?> targetType, Throwable cause) {
        super(message, cause);
        this.targetType = targetType;
    }

    /**
     * Returns the type that the body was being parsed to.
     *
     * @return the target class
     */
    public Class<?> targetType() {
        return targetType;
    }
}
