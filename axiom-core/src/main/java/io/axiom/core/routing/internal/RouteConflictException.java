package io.axiom.core.routing.internal;

import io.axiom.core.routing.Route;

/**
 * Exception thrown when a route conflicts with an existing route.
 *
 * <p>
 * Route conflicts occur when:
 * <ul>
 * <li>Two routes with same method and path are registered</li>
 * <li>Parameter name conflicts at the same path level</li>
 * </ul>
 *
 * @since 0.1.0
 */
public final class RouteConflictException extends RuntimeException {

    private final Route route;

    /**
     * Creates a conflict exception.
     *
     * @param route   the conflicting route
     * @param message description of the conflict
     */
    public RouteConflictException(Route route, String message) {
        super(message);
        this.route = route;
    }

    /**
     * Returns the route that caused the conflict.
     *
     * @return the conflicting route
     */
    public Route route() {
        return route;
    }
}
