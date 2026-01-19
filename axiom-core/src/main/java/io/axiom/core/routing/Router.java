package io.axiom.core.routing;

import java.util.*;
import java.util.function.*;

import org.slf4j.*;

import io.axiom.core.handler.*;
import io.axiom.core.middleware.*;
import io.axiom.core.routing.internal.*;

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

    private static final Logger LOG = LoggerFactory.getLogger(Router.class);

    private final RouteTrie trie;
    private final String basePath;
    private final List<MiddlewareHandler> middlewares;

    /**
     * Creates a new router with no base path.
     */
    public Router() {
        this(new RouteTrie(), "", new ArrayList<>());
    }

    private Router(final RouteTrie trie, final String basePath, final List<MiddlewareHandler> middlewares) {
        this.trie = trie;
        this.basePath = basePath;
        this.middlewares = middlewares;
    }

    // ========== Middleware ==========

    /**
     * Registers middleware for this router.
     *
     * <p>Middleware registered on a router applies to all routes in that router.
     * It wraps route handlers, executing before and optionally after.
     *
     * <pre>{@code
     * Router router = new Router();
     * router.use(authMiddleware::handle);  // Apply to all routes
     * router.get("/me", ctx -> ctx.json(user));
     * router.get("/profile", ctx -> ctx.json(profile));
     * }</pre>
     *
     * @param middleware the middleware handler
     * @return this router for chaining
     */
    public Router use(final MiddlewareHandler middleware) {
        Objects.requireNonNull(middleware, "Middleware cannot be null");
        this.middlewares.add(middleware);
        return this;
    }

    /**
     * Returns the middleware registered on this router.
     *
     * @return list of middleware handlers
     */
    public List<MiddlewareHandler> middlewares() {
        return List.copyOf(this.middlewares);
    }

    // ========== HTTP Method Shortcuts ==========

    /**
     * Registers a GET route.
     *
     * @param path    the path pattern
     * @param handler the request handler
     * @return this router for chaining
     */
    public Router get(final String path, final Handler handler) {
        return this.route("GET", path, handler);
    }

    /**
     * Registers a POST route.
     *
     * @param path    the path pattern
     * @param handler the request handler
     * @return this router for chaining
     */
    public Router post(final String path, final Handler handler) {
        return this.route("POST", path, handler);
    }

    /**
     * Registers a PUT route.
     *
     * @param path    the path pattern
     * @param handler the request handler
     * @return this router for chaining
     */
    public Router put(final String path, final Handler handler) {
        return this.route("PUT", path, handler);
    }

    /**
     * Registers a DELETE route.
     *
     * @param path    the path pattern
     * @param handler the request handler
     * @return this router for chaining
     */
    public Router delete(final String path, final Handler handler) {
        return this.route("DELETE", path, handler);
    }

    /**
     * Registers a PATCH route.
     *
     * @param path    the path pattern
     * @param handler the request handler
     * @return this router for chaining
     */
    public Router patch(final String path, final Handler handler) {
        return this.route("PATCH", path, handler);
    }

    /**
     * Registers a HEAD route.
     *
     * @param path    the path pattern
     * @param handler the request handler
     * @return this router for chaining
     */
    public Router head(final String path, final Handler handler) {
        return this.route("HEAD", path, handler);
    }

    /**
     * Registers an OPTIONS route.
     *
     * @param path    the path pattern
     * @param handler the request handler
     * @return this router for chaining
     */
    public Router options(final String path, final Handler handler) {
        return this.route("OPTIONS", path, handler);
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
    public Router route(final String method, final String path, final Handler handler) {
        Objects.requireNonNull(method, "Method cannot be null");
        Objects.requireNonNull(path, "Path cannot be null");
        Objects.requireNonNull(handler, "Handler cannot be null");

        final String fullPath = PathParser.join(this.basePath, path);
        final Route route = Route.of(method, fullPath, handler);
        this.trie.insert(route);

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

        if (!this.basePath.isEmpty()) {
            final String fullPath = PathParser.join(this.basePath, route.path());
            route = Route.of(route.method(), fullPath, route.handler());
        }

        this.trie.insert(route);
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
    public Router group(final String prefix, final Consumer<Router> configure) {
        Objects.requireNonNull(prefix, "Prefix cannot be null");
        Objects.requireNonNull(configure, "Configure function cannot be null");

        final String groupBasePath = PathParser.join(this.basePath, prefix);
        final Router groupRouter = new Router(this.trie, groupBasePath, new ArrayList<>(this.middlewares));
        configure.accept(groupRouter);

        return this;
    }

    // ========== Route Matching ==========

    /**
     * Matches a request against registered routes.
     *
     * @param method the HTTP method
     * @param path   the request path
     * @return Optional containing RouteMatch if found, empty otherwise
     */
    public Optional<RouteMatch> match(final String method, final String path) {
        final RouteMatch result = this.trie.match(method, path);
        if (Router.LOG.isDebugEnabled()) {
            if (result != null) {
                Router.LOG.debug("Route matched: {} {} -> {}", method, path, result.route().path());
            } else {
                Router.LOG.debug("No route matched: {} {}", method, path);
            }
        }
        return Optional.ofNullable(result);
    }

    /**
     * Returns all registered routes.
     *
     * <p>
     * Useful for debugging and route documentation.
     * The returned list is unmodifiable.
     *
     * @return unmodifiable list of all registered routes
     */
    public List<Route> routes() {
        return List.copyOf(this.trie.routes());
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
    public List<String> allowedMethods(final String path) {
        return this.trie.allowedMethods(path);
    }

    /**
     * Checks if a route exists for the given path (any method).
     *
     * @param path the request path
     * @return true if at least one route matches
     */
    public boolean hasRoute(final String path) {
        return this.trie.hasPath(path);
    }

    /**
     * Checks if a specific route exists.
     *
     * @param method the HTTP method
     * @param path   the request path
     * @return true if route exists
     */
    public boolean hasRoute(final String method, final String path) {
        return this.match(method, path).isPresent();
    }

    /**
     * Merges routes from another router into this one.
     *
     * @param other the router to merge
     * @return this router for chaining
     */
    public Router merge(final Router other) {
        Objects.requireNonNull(other, "Other router cannot be null");

        for (final Route route : other.routes()) {
            this.route(route);
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
    public Router merge(final String prefix, final Router other) {
        Objects.requireNonNull(prefix, "Prefix cannot be null");
        Objects.requireNonNull(other, "Other router cannot be null");

        for (final Route route : other.routes()) {
            final String fullPath = PathParser.join(prefix, route.path());
            this.route(route.method(), fullPath, route.handler());
        }

        return this;
    }
}
