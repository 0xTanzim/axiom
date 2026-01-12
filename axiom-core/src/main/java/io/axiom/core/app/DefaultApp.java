package io.axiom.core.app;

import java.util.*;
import java.util.function.*;

import io.axiom.core.context.*;
import io.axiom.core.error.*;
import io.axiom.core.handler.*;
import io.axiom.core.middleware.*;
import io.axiom.core.routing.*;

/**
 * Default implementation of {@link App}.
 *
 * <p>
 * This implementation handles middleware composition, route matching,
 * and error handling. It does NOT include an HTTP server — that is
 * provided by runtime adapters.
 *
 * <h2>Architecture</h2>
 * <p>
 * DefaultApp is the composition layer that:
 * <ul>
 * <li>Collects middleware and routes during configuration</li>
 * <li>Builds the handler chain when the app starts</li>
 * <li>Delegates HTTP serving to a runtime adapter</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * DefaultApp app = new DefaultApp();
 * app.use((ctx, next) -> { log(ctx.path()); next.run(); });
 * app.route(userRouter);
 * app.onError((ctx, e) -> { ctx.status(500); ctx.json(error(e)); });
 *
 * // For testing or manual integration
 * Handler handler = app.buildHandler();
 * }</pre>
 *
 * @since 0.1.0
 */
public class DefaultApp implements App {

    private final List<Middleware> middlewares;
    private final Router router;
    private ErrorHandler errorHandler;

    private volatile boolean started;
    private volatile Handler composedHandler;

    public DefaultApp() {
        this.middlewares = new ArrayList<>();
        this.router = new Router();
        this.errorHandler = DefaultApp::defaultErrorHandler;
        this.started = false;
    }

    // ========== Middleware ==========

    @Override
    public App use(final MiddlewareHandler middleware) {
        this.ensureNotStarted();
        Objects.requireNonNull(middleware, "Middleware cannot be null");
        this.middlewares.add(MiddlewareAdapter.adapt(middleware));
        return this;
    }

    @Override
    public App use(final SimpleMiddleware middleware) {
        this.ensureNotStarted();
        Objects.requireNonNull(middleware, "Middleware cannot be null");
        this.middlewares.add(MiddlewareAdapter.adapt(middleware));
        return this;
    }

    @Override
    public App before(final Handler hook) {
        this.ensureNotStarted();
        Objects.requireNonNull(hook, "Before hook cannot be null");
        this.middlewares.add(MiddlewareAdapter.before(hook));
        return this;
    }

    @Override
    public App after(final Handler hook) {
        this.ensureNotStarted();
        Objects.requireNonNull(hook, "After hook cannot be null");
        this.middlewares.add(MiddlewareAdapter.after(hook));
        return this;
    }

    // ========== Routing ==========

    @Override
    public App route(final Router router) {
        this.ensureNotStarted();
        Objects.requireNonNull(router, "Router cannot be null");
        this.router.merge(router);
        return this;
    }

    @Override
    public App route(final String basePath, final Router router) {
        this.ensureNotStarted();
        Objects.requireNonNull(basePath, "Base path cannot be null");
        Objects.requireNonNull(router, "Router cannot be null");
        this.router.merge(basePath, router);
        return this;
    }

    @Override
    public App route(final Supplier<Router> supplier) {
        this.ensureNotStarted();
        Objects.requireNonNull(supplier, "Router supplier cannot be null");
        final Router r = supplier.get();
        if (r != null) {
            this.router.merge(r);
        }
        return this;
    }

    @Override
    public App route(final String basePath, final Supplier<Router> supplier) {
        this.ensureNotStarted();
        Objects.requireNonNull(basePath, "Base path cannot be null");
        Objects.requireNonNull(supplier, "Router supplier cannot be null");
        final Router r = supplier.get();
        if (r != null) {
            this.router.merge(basePath, r);
        }
        return this;
    }

    // ========== Error Handling ==========

    @Override
    public App onError(final ErrorHandler handler) {
        this.ensureNotStarted();
        Objects.requireNonNull(handler, "Error handler cannot be null");
        this.errorHandler = handler;
        return this;
    }

    // ========== Lifecycle ==========

    @Override
    public void listen(final int port) {
        this.listen("0.0.0.0", port);
    }

    @Override
    public void listen(final String host, final int port) {
        throw new UnsupportedOperationException(
                "DefaultApp does not include an HTTP server. " +
                        "Use a runtime adapter (e.g., JdkServerApp) or call buildHandler() for testing.");
    }

    @Override
    public int port() {
        return -1;
    }

    @Override
    public void stop() {
        this.started = false;
    }

    @Override
    public boolean isRunning() {
        return this.started;
    }

    // ========== Handler Building ==========

    /**
     * Builds the composed request handler.
     *
     * <p>
     * This method composes all middleware and routes into a single
     * handler that can be used by runtime adapters or for testing.
     *
     * @return the composed handler
     */
    public Handler buildHandler() {
        if (this.composedHandler == null) {
            this.composedHandler = this.composeHandler();
        }
        return this.composedHandler;
    }

    /**
     * Returns the configured router.
     *
     * <p>
     * Useful for introspection and testing.
     *
     * @return the router with all registered routes
     */
    public Router getRouter() {
        return this.router;
    }

    /**
     * Returns the configured error handler.
     *
     * @return the error handler
     */
    public ErrorHandler getErrorHandler() {
        return this.errorHandler;
    }

    // ========== Internal ==========

    private Handler composeHandler() {
        // Core routing handler
        final Handler routingHandler = this::handleRequest;

        // Wrap with error handling
        final Handler withErrorHandling = ctx -> {
            try {
                routingHandler.handle(ctx);
            } catch (final Exception e) {
                this.handleError(ctx, e);
            }
        };

        // Apply middleware in reverse order (last registered = innermost)
        Handler result = withErrorHandling;
        for (int i = this.middlewares.size() - 1; i >= 0; i--) {
            result = this.middlewares.get(i).apply(result);
        }

        return result;
    }

    private void handleRequest(final Context ctx) throws Exception {
        final String method = ctx.method();
        final String path = ctx.path();

        final RouteMatch match = this.router.match(method, path);

        if (match == null) {
            // Check if path exists with different method
            if (this.router.hasRoute(path)) {
                final List<String> allowed = this.router.allowedMethods(path);
                throw new MethodNotAllowedException(method, path, allowed);
            }
            throw new RouteNotFoundException(method, path);
        }

        // Set path parameters on context
        if (ctx instanceof final io.axiom.core.context.DefaultContext defaultCtx) {
            defaultCtx.setPathParams(match.params());
        }

        // Execute handler
        match.route().handler().handle(ctx);
    }

    private void handleError(final Context ctx, final Exception e) {
        try {
            this.errorHandler.handle(ctx, e);
        } catch (final Exception errorHandlerException) {
            // Error handler itself failed - last resort
            System.err.println("Error handler failed: " + errorHandlerException.getMessage());
            errorHandlerException.printStackTrace();
            try {
                ctx.status(500);
                ctx.text("Internal Server Error");
            } catch (final Exception ignored) {
                // Response may already be committed
            }
        }
    }

    private void ensureNotStarted() {
        if (this.started) {
            throw new IllegalStateException(
                    "Cannot modify app after it has started. " +
                            "Configure all middleware and routes before calling listen().");
        }
    }

    // ========== Default Error Handler ==========

    private static void defaultErrorHandler(final Context ctx, final Exception e) {
        if (e instanceof final RouteNotFoundException notFound) {
            ctx.status(404);
            ctx.json(Map.of(
                    "error", "Not Found",
                    "method", notFound.method(),
                    "path", notFound.path()));
        } else if (e instanceof final MethodNotAllowedException notAllowed) {
            ctx.status(405);
            ctx.header("Allow", String.join(", ", notAllowed.allowedMethods()));
            ctx.json(Map.of(
                    "error", "Method Not Allowed",
                    "path", notAllowed.path(),
                    "allowed", notAllowed.allowedMethods()));
        } else if (e instanceof BodyParseException) {
            ctx.status(400);
            ctx.json(Map.of(
                    "error", "Bad Request",
                    "message", e.getMessage()));
        } else if (e instanceof ResponseCommittedException) {
            // Response already sent - nothing we can do
            System.err.println("Response already committed: " + e.getMessage());
        } else {
            // Unknown error
            ctx.status(500);
            ctx.json(Map.of(
                    "error", "Internal Server Error",
                    "message", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }
}
