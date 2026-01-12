package io.axiom.core.routing;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import io.axiom.core.handler.Handler;
import io.axiom.core.routing.internal.RouteTrie;

/**
 * Route registration and matching API.
 *
 * <p>
 * Router is the primary API for defining HTTP routes in Axiom.
 * It provides a fluent, functional interface for binding handlers
 * to HTTP methods and path patterns.
 *
 * <h2>Basic Usage</h2>
 * 
 * <pre>{@code
 * Router router = new Router();
 *
 * router.get("/health", c -> c.text("OK"));
 *
 * router.get("/users/:id", c -> {
 *     String id = c.param("id");
 *     c.json(userService.find(id));
 * });
 *
 * router.post("/users", c -> {
 *     User user = c.body(User.class);
 *     c.status(201);
 *     c.json(userService.create(user));
 * });
 * }</pre>
 *
 * <h2>Route Grouping</h2>
 * 
 * <pre>{@code
 * router.group("/api/v1", api -> {
 *     api.get("/users", c -> c.json(userService.list()));
 *     api.get("/users/:id", c -> c.json(userService.find(c.param("id"))));
 *
 *     api.group("/admin", admin -> {
 *         admin.get("/stats", c -> c.json(statsService.get()));
 *     });
 * });
 * }</pre>
 *
 * <h2>Path Patterns</h2>
 * <ul>
 * <li>{@code /users} - Static path</li>
 * <li>{@code /users/:id} - Named parameter</li>
 * <li>{@code /files/*} - Wildcard (catch-all)</li>
 * </ul>
 *
 * @see Route
 * @see RouteMatch
 * @since 0.1.0
 */
public final class Router {

    private final RouteTrie trie;
    private final String basePath;

    /**
     * Creates a new router with no base path.
     */
    public Router() {
        this(new RouteTrie(), "");
    }

    private Router(RouteTrie trie, String basePath) {
        this.trie = trie;
        this.basePath = basePath;
    }

    // ========== HTTP Method Shortcuts ==========

    /**
     * Registers a GET route.
     *
     * @param path    the path pattern
     * @param handler the request handler
     * @return this router for chaining
     */
    public Router get(String path, Handler handler) {
        return route("GET", path, handler);
    }

    /**
     * Registers a POST route.
     *
     * @param path    the path pattern
     * @param handler the request handler
     * @return this router for chaining
     */
    public Router post(String path, Handler handler) {
        return route("POST", path, handler);
    }

    /**
     * Registers a PUT route.
     *
     * @param path    the path pattern
     * @param handler the request handler
     * @return this router for chaining
     */
    public Router put(String path, Handler handler) {
        return route("PUT", path, handler);
    }

    /**
     * Registers a DELETE route.
     *
     * @param path    the path pattern
     * @param handler the request handler
     * @return this router for chaining
     */
    public Router delete(String path, Handler handler) {
        return route("DELETE", path, handler);
    }

    /**
     * Registers a PATCH route.
     *
     * @param path    the path pattern
     * @param handler the request handler
     * @return this router for chaining
     */
    public Router patch(String path, Handler handler) {
        return route("PATCH", path, handler);
    }

    /**
     * Registers a HEAD route.
     *
     * @param path    the path pattern
     * @param handler the request handler
     * @return this router for chaining
     */
    public Router head(String path, Handler handler) {
        return route("HEAD", path, handler);
    }

    /**
     * Registers an OPTIONS route.
     *
     * @param path    the path pattern
     * @param handler the request handler
     * @return this router for chaining
     */
    public Router options(String path, Handler handler) {
        return route("OPTIONS", path, handler);
    }

    // ========== Generic Route Registration ==========

    /**
     * Registers a route for any HTTP method.
     *
     * @param method  the HTTP method
     * @param path    the path pattern
     * @param handler the request handler
     * @return this router for chaining
     */
    public Router route(String method, String path, Handler handler) {
        Objects.requireNonNull(method, "Method cannot be null");
        Objects.requireNonNull(path, "Path cannot be null");
        Objects.requireNonNull(handler, "Handler cannot be null");

        String fullPath = PathParser.join(basePath, path);
        Route route = Route.of(method, fullPath, handler);
        trie.insert(route);

        return this;
    }

    /**
     * Registers a pre-built route.
     *
     * @param route the route to register
     * @return this router for chaining
     */
    public Router route(Route route) {
        Objects.requireNonNull(route, "Route cannot be null");

        if (!basePath.isEmpty()) {
            String fullPath = PathParser.join(basePath, route.path());
            route = Route.of(route.method(), fullPath, route.handler());
        }

        trie.insert(route);
        return this;
    }

    // ========== Route Grouping ==========

    /**
     * Creates a route group with a path prefix.
     *
     * <p>
     * All routes registered within the group will have the
     * base path prepended automatically.
     *
     * <pre>{@code
     * router.group("/api", api -> {
     *     api.get("/users", handler); // Becomes /api/users
     *     api.get("/posts", handler); // Becomes /api/posts
     * });
     * }</pre>
     *
     * @param prefix    the path prefix for all routes in the group
     * @param configure function to configure routes in the group
     * @return this router for chaining
     */
    public Router group(String prefix, Consumer<Router> configure) {
        Objects.requireNonNull(prefix, "Prefix cannot be null");
        Objects.requireNonNull(configure, "Configure function cannot be null");

        String groupBasePath = PathParser.join(basePath, prefix);
        Router groupRouter = new Router(trie, groupBasePath);
        configure.accept(groupRouter);

        return this;
    }

    // ========== Route Matching ==========

    /**
     * Matches a request against registered routes.
     *
     * @param method the HTTP method
     * @param path   the request path
     * @return RouteMatch if found, null otherwise
     */
    public RouteMatch match(String method, String path) {
        return trie.match(method, path);
    }

    /**
     * Returns all registered routes.
     *
     * <p>
     * Useful for debugging and route documentation.
     *
     * @return list of all routes
     */
    public List<Route> routes() {
        return trie.routes();
    }

    /**
     * Returns HTTP methods allowed for a path.
     *
     * <p>
     * Useful for generating Allow headers in 405 responses.
     *
     * @param path the request path
     * @return list of allowed HTTP methods
     */
    public List<String> allowedMethods(String path) {
        return trie.allowedMethods(path);
    }

    /**
     * Checks if a route exists for the given path (any method).
     *
     * @param path the request path
     * @return true if at least one route matches
     */
    public boolean hasRoute(String path) {
        return trie.hasPath(path);
    }

    /**
     * Checks if a specific route exists.
     *
     * @param method the HTTP method
     * @param path   the request path
     * @return true if route exists
     */
    public boolean hasRoute(String method, String path) {
        return match(method, path) != null;
    }

    /**
     * Merges routes from another router into this one.
     *
     * @param other the router to merge
     * @return this router for chaining
     */
    public Router merge(Router other) {
        Objects.requireNonNull(other, "Other router cannot be null");

        for (Route route : other.routes()) {
            route(route);
        }

        return this;
    }

    /**
     * Merges routes from another router with a path prefix.
     *
     * @param prefix the path prefix
     * @param other  the router to merge
     * @return this router for chaining
     */
    public Router merge(String prefix, Router other) {
        Objects.requireNonNull(prefix, "Prefix cannot be null");
        Objects.requireNonNull(other, "Other router cannot be null");

        for (Route route : other.routes()) {
            String fullPath = PathParser.join(prefix, route.path());
            route(route.method(), fullPath, route.handler());
        }

        return this;
    }
}
