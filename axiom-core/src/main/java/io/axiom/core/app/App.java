package io.axiom.core.app;

import java.util.function.*;

import io.axiom.core.error.*;
import io.axiom.core.handler.*;
import io.axiom.core.middleware.*;
import io.axiom.core.routing.*;

/**
 * Main application interface for Axiom framework.
 *
 * <p>
 * App is the entry point for building web applications with Axiom.
 * It provides methods for registering middleware, routes, error handlers,
 * and controlling the application lifecycle.
 *
 * <h2>Basic Usage</h2>
 *
 * <pre>{@code
 * Router router = new Router();
 * router.get("/health", ctx -> ctx.text("OK"));
 * router.get("/users/:id", ctx -> ctx.json(userService.find(ctx.param("id"))));
 *
 * App app = Axiom.create();
 * app.use(logging());
 * app.route(router);
 * app.listen(8080);
 * }</pre>
 *
 * <h2>Middleware Styles</h2>
 *
 * <p>
 * Axiom supports two middleware styles. Use whichever fits your mental model:
 *
 * <pre>{@code
 * // Style 1: Explicit next parameter
 * app.use((ctx, next) -> {
 *     log.info("Request: {} {}", ctx.method(), ctx.path());
 *     next.run();
 * });
 *
 * // Style 2: Context-embedded next
 * app.use(ctx -> {
 *     log.info("Request: {} {}", ctx.method(), ctx.path());
 *     ctx.next();
 * });
 * }</pre>
 *
 * <h2>Before/After Hooks</h2>
 *
 * <pre>{@code
 * app.before(ctx -> log.debug("Before handler"));
 * app.after(ctx -> log.debug("After handler"));
 * }</pre>
 *
 * <h2>Error Handling</h2>
 *
 * <pre>{@code
 * app.onError((ctx, e) -> {
 *     ctx.status(500);
 *     ctx.json(Map.of("error", e.getMessage()));
 * });
 * }</pre>
 *
 * <h2>Lifecycle</h2>
 *
 * <pre>{@code
 * app.listen(8080); // Start server (blocking)
 * app.stop(); // Graceful shutdown
 * }</pre>
 *
 * @see Router
 * @see MiddlewareHandler
 * @see SimpleMiddleware
 * @see ErrorHandler
 * @since 0.1.0
 */
public interface App {

    // ========== Middleware ==========

    /**
     * Registers global middleware with explicit next parameter.
     *
     * <p>
     * Middleware is executed in registration order for every request.
     * It wraps all route handlers.
     *
     * <pre>{@code
     * app.use((ctx, next) -> {
     *     log(ctx.path());
     *     next.run();
     * });
     * }</pre>
     *
     * @param middleware the middleware handler with (ctx, next) signature
     * @return this app for chaining
     */
    App use(MiddlewareHandler middleware);

    /**
     * Registers global middleware with context-embedded next.
     *
     * <p>
     * Alternative style where next() is called on the context.
     *
     * <pre>{@code
     * app.use(ctx -> {
     *     log(ctx.path());
     *     ctx.next();
     * });
     * }</pre>
     *
     * @param middleware the middleware handler with ctx.next() style
     * @return this app for chaining
     */
    App use(SimpleMiddleware middleware);

    /**
     * Registers a before-handler hook.
     *
     * <p>
     * Before hooks run after middleware but before the route handler.
     * They cannot short-circuit the request.
     *
     * @param hook the before handler
     * @return this app for chaining
     */
    App before(Handler hook);

    /**
     * Registers an after-handler hook.
     *
     * <p>
     * After hooks run after the route handler completes successfully.
     * They are useful for cleanup or logging.
     *
     * @param hook the after handler
     * @return this app for chaining
     */
    App after(Handler hook);

    // ========== Routing ==========

    /**
     * Registers routes from a router.
     *
     * @param router the router containing routes
     * @return this app for chaining
     */
    App route(Router router);

    /**
     * Registers routes from a router with a path prefix.
     *
     * @param basePath the base path prefix
     * @param router   the router containing routes
     * @return this app for chaining
     */
    App route(String basePath, Router router);

    /**
     * Registers routes from a router supplier.
     *
     * <p>
     * Using a supplier allows lazy initialization of routes,
     * which is useful for modular applications.
     *
     * @param supplier supplies the router
     * @return this app for chaining
     */
    App route(Supplier<Router> supplier);

    /**
     * Registers routes from a router supplier with a path prefix.
     *
     * @param basePath the base path prefix
     * @param supplier supplies the router
     * @return this app for chaining
     */
    App route(String basePath, Supplier<Router> supplier);

    // ========== Error Handling ==========

    /**
     * Registers a global error handler.
     *
     * <p>
     * The error handler receives all uncaught exceptions from
     * handlers and middleware. Only one error handler can be registered.
     *
     * @param handler the error handler
     * @return this app for chaining
     */
    App onError(ErrorHandler handler);

    // ========== Lifecycle ==========

    /**
     * Starts the server on the specified port.
     *
     * <p>
     * This method blocks until the server is stopped.
     *
     * @param port the port to listen on
     */
    void listen(int port);

    /**
     * Starts the server on the specified host and port.
     *
     * @param host the host to bind to
     * @param port the port to listen on
     */
    void listen(String host, int port);

    /**
     * Returns the port the server is listening on.
     *
     * <p>
     * Useful when port 0 is used for automatic port assignment.
     *
     * @return the actual port, or -1 if not started
     */
    int port();

    /**
     * Initiates graceful shutdown.
     *
     * <p>
     * The server will stop accepting new connections and wait
     * for existing requests to complete.
     */
    void stop();

    /**
     * Checks if the server is currently running.
     *
     * @return true if server is accepting requests
     */
    boolean isRunning();
}
