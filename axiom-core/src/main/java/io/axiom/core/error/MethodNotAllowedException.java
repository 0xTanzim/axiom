package io.axiom.core.error;

import java.util.List;

/**
 * Exception thrown when method is not allowed for a matched path.
 *
 * <p>
 * Corresponds to HTTP 405 Method Not Allowed.
 *
 * <p>
 * This exception includes the list of allowed methods, which should
 * be returned in the "Allow" response header.
 *
 * <h2>Usage</h2>
 * 
 * <pre>{@code
 * app.onError((c, e) -> {
 *     if (e instanceof MethodNotAllowedException notAllowed) {
 *         c.status(405);
 *         c.header("Allow", String.join(", ", notAllowed.allowedMethods()));
 *         c.json(Map.of(
 *                 "error", "Method Not Allowed",
 *                 "allowed", notAllowed.allowedMethods()));
 *     }
 * });
 * }</pre>
 *
 * @since 0.1.0
 */
public final class MethodNotAllowedException extends AxiomException {

    private final String method;
    private final String path;
    private final List<String> allowedMethods;

    /**
     * Creates a method not allowed exception.
     *
     * @param method         the attempted HTTP method
     * @param path           the request path
     * @param allowedMethods methods that are allowed for this path
     */
    public MethodNotAllowedException(String method, String path, List<String> allowedMethods) {
        super("Method " + method + " not allowed for " + path
                + ". Allowed: " + String.join(", ", allowedMethods));
        this.method = method;
        this.path = path;
        this.allowedMethods = List.copyOf(allowedMethods);
    }

    /**
     * Returns the attempted HTTP method.
     *
     * @return the HTTP method
     */
    public String method() {
        return method;
    }

    /**
     * Returns the request path.
     *
     * @return the path
     */
    public String path() {
        return path;
    }

    /**
     * Returns the methods allowed for this path.
     *
     * <p>
     * Use this to populate the "Allow" response header.
     *
     * @return list of allowed HTTP methods
     */
    public List<String> allowedMethods() {
        return allowedMethods;
    }
}
