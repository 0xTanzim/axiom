package io.axiom.core.error;

/**
 * Base exception for all Axiom framework errors.
 *
 * <p>
 * All framework-specific exceptions extend this class, enabling
 * catch-all handling for Axiom errors while distinguishing them
 * from application exceptions.
 *
 * <h2>Exception Hierarchy</h2>
 * 
 * <pre>
 * AxiomException
 * ├── RouteNotFoundException       (404)
 * ├── MethodNotAllowedException    (405)
 * ├── BodyParseException           (400)
 * └── ResponseCommittedException   (programming error)
 * </pre>
 *
 * <h2>Error Handling</h2>
 * 
 * <pre>{@code
 * app.onError((c, e) -> {
 *     if (e instanceof RouteNotFoundException) {
 *         c.status(404);
 *         c.json(Map.of("error", "Not Found"));
 *     } else if (e instanceof AxiomException) {
 *         c.status(400);
 *         c.json(Map.of("error", e.getMessage()));
 *     } else {
 *         c.status(500);
 *         c.json(Map.of("error", "Internal Server Error"));
 *     }
 * });
 * }</pre>
 *
 * @since 0.1.0
 */
public class AxiomException extends RuntimeException {

    /**
     * Creates an exception with a message.
     *
     * @param message the error message
     */
    public AxiomException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public AxiomException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception with a cause.
     *
     * @param cause the underlying cause
     */
    public AxiomException(Throwable cause) {
        super(cause);
    }
}
