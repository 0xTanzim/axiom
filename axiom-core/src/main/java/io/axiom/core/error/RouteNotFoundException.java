package io.axiom.core.error;

/**
 * Exception thrown when no route matches the request.
 *
 * <p>
 * Corresponds to HTTP 404 Not Found.
 *
 * <h2>Usage</h2>
 * 
 * <pre>{@code
 * app.onError((c, e) -> {
 *     if (e instanceof RouteNotFoundException notFound) {
 *         c.status(404);
 *         c.json(Map.of(
 *                 "error", "Not Found",
 *                 "path", notFound.path(),
 *                 "method", notFound.method()));
 *     }
 * });
 * }</pre>
 *
 * @since 0.1.0
 */
public final class RouteNotFoundException extends AxiomException {

    private final String method;
    private final String path;

    /**
     * Creates a route not found exception.
     *
     * @param method the HTTP method
     * @param path   the request path
     */
    public RouteNotFoundException(String method, String path) {
        super("No route found for " + method + " " + path);
        this.method = method;
        this.path = path;
    }

    /**
     * Returns the HTTP method of the unmatched request.
     *
     * @return the HTTP method
     */
    public String method() {
        return method;
    }

    /**
     * Returns the path of the unmatched request.
     *
     * @return the request path
     */
    public String path() {
        return path;
    }
}
