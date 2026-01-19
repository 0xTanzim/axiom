package io.axiom.core.app;

import java.util.function.*;

import io.axiom.core.error.*;
import io.axiom.core.handler.*;
import io.axiom.core.lifecycle.*;
import io.axiom.core.middleware.*;
import io.axiom.core.routing.*;
import io.axiom.core.server.*;

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

    // ========== Lifecycle Hooks ==========

    /**
     * Registers a startup hook.
     *
     * <p>
     * Runs during STARTING phase, before server accepts requests.
     * Use for: database connections, cache warming, validation.
     *
     * <pre>{@code
     * app.onStart(() -> {
     *     database.connect();
     *     cache.warm();
     * });
     * }</pre>
     *
     * @param action the startup action
     * @return this app for chaining
     */
    App onStart(ThrowingRunnable action);

    /**
     * Registers a ready hook.
     *
     * <p>
     * Runs after STARTED, when server is accepting requests.
     * Use for: logging, health check registration, metrics.
     * Should be fast and non-blocking.
     *
     * <pre>{@code
     * app.onReady(() -> {
     *     log.info("Server ready at http://localhost:{}", app.port());
     * });
     * }</pre>
     *
     * @param action the ready action
     * @return this app for chaining
     */
    App onReady(Runnable action);

    /**
     * Registers a shutdown hook.
     *
     * <p>
     * Runs during STOPPING phase, in reverse registration order.
     * Use for: cleanup, connection closing, flush buffers.
     *
     * <pre>{@code
     * app.onShutdown(() -> {
     *     database.close();
     *     cache.flush();
     * });
     * }</pre>
     *
     * @param action the shutdown action
     * @return this app for chaining
     */
    App onShutdown(ThrowingRunnable action);

    /**
     * Registers a lifecycle error hook.
     *
     * <p>
     * Runs when ERROR state is entered due to lifecycle failure.
     * Use for: alerting, cleanup attempts, logging.
     *
     * <pre>{@code
     * app.onLifecycleError(e -> {
     *     alerting.send("Server failed: " + e.getMessage());
     * });
     * }</pre>
     *
     * @param action the error handler
     * @return this app for chaining
     */
    App onLifecycleError(Consumer<Throwable> action);

    // ========== Lifecycle Control ==========

    /**
     * Starts the server on the specified port.
     *
     * <p>
     * <strong>Note:</strong> This method blocks the calling thread until
     * {@link #stop()} is called or the JVM shuts down. For non-blocking
     * startup, use a separate thread or virtual thread.
     *
     * <pre>{@code
     * // Blocking (typical for main method)
     * app.listen(8080);
     *
     * // Non-blocking (for testing or embedded use)
     * Thread.startVirtualThread(() -> app.listen(8080));
     * }</pre>
     *
     * @param port the port to listen on
     * @throws StartupException if startup fails
     */
    void listen(int port);

    /**
     * Starts the server on the specified host and port.
     *
     * <p>
     * <strong>Note:</strong> This method blocks until shutdown.
     * See {@link #listen(int)} for details.
     *
     * @param host the host to bind to
     * @param port the port to listen on
     * @throws StartupException if startup fails
     */
    void listen(String host, int port);

    /**
     * Starts the server with custom configuration.
     *
     * <p>
     * <strong>Note:</strong> This method blocks until shutdown.
     * See {@link #listen(int)} for details.
     *
     * @param config the server configuration
     * @throws StartupException if startup fails
     */
    void listen(ServerConfig config);

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
     * for existing requests to complete up to the configured timeout.
     */
    void stop();

    /**
     * Checks if the server is currently running.
     *
     * @return true if server is accepting requests
     */
    boolean isRunning();

    /**
     * Returns the current lifecycle phase.
     *
     * @return current phase
     */
    LifecyclePhase phase();
}
