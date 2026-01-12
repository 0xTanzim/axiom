package io.axiom.core.routing;

import java.util.Map;
import java.util.Objects;

/**
 * Result of a successful route match.
 *
 * <p>
 * Contains the matched route and any extracted path parameters.
 *
 * <h2>Example</h2>
 * 
 * <pre>{@code
 * RouteMatch match = router.match("GET", "/users/123");
 * if (match != null) {
 *     Route route = match.route();
 *     String id = match.params().get("id"); // "123"
 *     route.handler().handle(context);
 * }
 * }</pre>
 *
 * @param route  the matched route
 * @param params extracted path parameters (immutable)
 * @since 0.1.0
 */
public record RouteMatch(
        Route route,
        Map<String, String> params) {
    /**
     * Creates a route match with validation.
     */
    public RouteMatch {
        Objects.requireNonNull(route, "Route cannot be null");
        Objects.requireNonNull(params, "Params cannot be null");
        params = Map.copyOf(params);
    }

    /**
     * Creates a match with no parameters.
     *
     * @param route the matched route
     * @return match with empty params map
     */
    public static RouteMatch of(Route route) {
        return new RouteMatch(route, Map.of());
    }

    /**
     * Creates a match with parameters.
     *
     * @param route  the matched route
     * @param params the extracted parameters
     * @return match with parameters
     */
    public static RouteMatch of(Route route, Map<String, String> params) {
        return new RouteMatch(route, params);
    }

    /**
     * Gets a parameter value by name.
     *
     * @param name the parameter name
     * @return the parameter value, or null if not present
     */
    public String param(String name) {
        return params.get(name);
    }

    /**
     * Checks if parameters were extracted.
     *
     * @return true if params map is not empty
     */
    public boolean hasParams() {
        return !params.isEmpty();
    }
}
