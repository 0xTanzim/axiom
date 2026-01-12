package io.axiom.core.routing;

import java.util.List;
import java.util.Objects;

import io.axiom.core.handler.Handler;

/**
 * Immutable representation of a registered route.
 *
 * <p>
 * A route binds an HTTP method and path pattern to a handler.
 * Routes are created at registration time and stored in the route trie.
 *
 * <h2>Path Patterns</h2>
 * 
 * <pre>
 * "/users"           - Static path
 * "/users/:id"       - Path with parameter
 * "/files/*"         - Path with wildcard
 * "/api/:version/users/:id" - Multiple parameters
 * </pre>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * Route route = Route.of("GET", "/users/:id", c -> {
 *     c.json(userService.find(c.param("id")));
 * });
 *
 * // Access route metadata
 * String method = route.method(); // "GET"
 * String path = route.path(); // "/users/:id"
 * List<Segment> segments = route.segments();
 * }</pre>
 *
 * @param method   the HTTP method (GET, POST, PUT, DELETE, etc.)
 * @param path     the original path pattern
 * @param segments pre-parsed path segments
 * @param handler  the request handler
 * @since 0.1.0
 */
public record Route(
        String method,
        String path,
        List<Segment> segments,
        Handler handler) {
    /**
     * Creates a route with validation.
     *
     * @param method   HTTP method
     * @param path     path pattern
     * @param segments pre-parsed segments
     * @param handler  request handler
     */
    public Route {
        Objects.requireNonNull(method, "Method cannot be null");
        Objects.requireNonNull(path, "Path cannot be null");
        Objects.requireNonNull(segments, "Segments cannot be null");
        Objects.requireNonNull(handler, "Handler cannot be null");

        method = method.toUpperCase();

        if (!isValidMethod(method)) {
            throw new IllegalArgumentException("Invalid HTTP method: " + method);
        }

        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Path must start with '/': " + path);
        }

        // Ensure segments list is immutable
        segments = List.copyOf(segments);
    }

    /**
     * Creates a route from method, path, and handler.
     *
     * <p>
     * Path is automatically parsed into segments.
     *
     * @param method  the HTTP method
     * @param path    the path pattern
     * @param handler the handler
     * @return new route instance
     */
    public static Route of(String method, String path, Handler handler) {
        return new Route(
                method,
                path,
                PathParser.parse(path),
                handler);
    }

    /**
     * Creates a GET route.
     */
    public static Route get(String path, Handler handler) {
        return of("GET", path, handler);
    }

    /**
     * Creates a POST route.
     */
    public static Route post(String path, Handler handler) {
        return of("POST", path, handler);
    }

    /**
     * Creates a PUT route.
     */
    public static Route put(String path, Handler handler) {
        return of("PUT", path, handler);
    }

    /**
     * Creates a DELETE route.
     */
    public static Route delete(String path, Handler handler) {
        return of("DELETE", path, handler);
    }

    /**
     * Creates a PATCH route.
     */
    public static Route patch(String path, Handler handler) {
        return of("PATCH", path, handler);
    }

    /**
     * Checks if this route has any parameter segments.
     *
     * @return true if route contains parameters
     */
    public boolean hasParams() {
        return segments.stream().anyMatch(s -> s instanceof ParamSegment);
    }

    /**
     * Checks if this route has a wildcard segment.
     *
     * @return true if route ends with wildcard
     */
    public boolean hasWildcard() {
        return !segments.isEmpty()
                && segments.getLast() instanceof WildcardSegment;
    }

    /**
     * Returns the number of segments in this route.
     *
     * @return segment count
     */
    public int segmentCount() {
        return segments.size();
    }

    private static boolean isValidMethod(String method) {
        return switch (method) {
            case "GET", "POST", "PUT", "DELETE", "PATCH",
                    "HEAD", "OPTIONS", "TRACE", "CONNECT" ->
                true;
            default -> false;
        };
    }

    @Override
    public String toString() {
        return method + " " + path;
    }
}
