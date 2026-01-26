package io.axiom.core.app;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.slf4j.*;

import io.axiom.core.context.*;
import io.axiom.core.error.*;
import io.axiom.core.handler.*;
import io.axiom.core.lifecycle.*;
import io.axiom.core.middleware.*;
import io.axiom.core.routing.*;
import io.axiom.core.server.*;

/**
 * Default implementation of {@link App}.
 *
 * <p>
 * This implementation handles middleware composition, route matching,
 * error handling, and lifecycle management. The HTTP server is provided
 * by runtime adapters discovered via {@link java.util.ServiceLoader}.
 *
 * <h2>Architecture</h2>
 * <p>
 * DefaultApp is the composition layer that:
 * <ul>
 * <li>Collects middleware and routes during configuration</li>
 * <li>Manages lifecycle hooks (start, ready, shutdown, error)</li>
 * <li>Builds the handler chain when the app starts</li>
 * <li>Auto-discovers server runtime via SPI</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * App app = Axiom.create();
 * app.onStart(() -> database.connect());
 * app.onShutdown(() -> database.close());
 * app.use((ctx, next) -> { log(ctx.path()); next.run(); });
 * app.route(userRouter);
 * app.listen(8080);
 * }</pre>
 *
 * <h2>Testing</h2>
 *
 * <pre>{@code
 * DefaultApp app = Axiom.createDefault();
 * Handler handler = app.buildHandler();  // No server needed
 * }</pre>
 *
 * @since 0.1.0
 */
public class DefaultApp implements App {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultApp.class);

    private final List<MiddlewareFunction> middlewares;
    private final Router router;
    private final List<ThrowingRunnable> startHooks;
    private final List<Runnable> readyHooks;
    private final List<ThrowingRunnable> shutdownHooks;
    private final List<Consumer<Throwable>> errorHooks;

    private ErrorHandler errorHandler;
    private ServerConfig serverConfig;
    private final AtomicReference<LifecyclePhase> phase;
    private volatile Handler composedHandler;
    private volatile Server activeServer;

    public DefaultApp() {
        this.middlewares = new ArrayList<>();
        this.router = new Router();
        this.startHooks = new ArrayList<>();
        this.readyHooks = new ArrayList<>();
        this.shutdownHooks = new ArrayList<>();
        this.errorHooks = new ArrayList<>();
        this.errorHandler = DefaultApp::defaultErrorHandler;
        this.serverConfig = ServerConfig.defaults();
        this.phase = new AtomicReference<>(LifecyclePhase.INIT);
    }

    // ========== Middleware ==========

    @Override
    public App use(final MiddlewareHandler middleware) {
        this.ensurePhase(LifecyclePhase.INIT);
        Objects.requireNonNull(middleware, "Middleware cannot be null");
        this.middlewares.add(MiddlewareAdapter.adapt(middleware));
        return this;
    }

    @Override
    public App use(final SimpleMiddleware middleware) {
        this.ensurePhase(LifecyclePhase.INIT);
        Objects.requireNonNull(middleware, "Middleware cannot be null");
        this.middlewares.add(MiddlewareAdapter.adapt(middleware));
        return this;
    }

    @Override
    public App before(final Handler hook) {
        this.ensurePhase(LifecyclePhase.INIT);
        Objects.requireNonNull(hook, "Before hook cannot be null");
        this.middlewares.add(MiddlewareAdapter.before(hook));
        return this;
    }

    @Override
    public App after(final Handler hook) {
        this.ensurePhase(LifecyclePhase.INIT);
        Objects.requireNonNull(hook, "After hook cannot be null");
        this.middlewares.add(MiddlewareAdapter.after(hook));
        return this;
    }

    // ========== Routing ==========

    @Override
    public App route(final Router router) {
        this.ensurePhase(LifecyclePhase.INIT);
        Objects.requireNonNull(router, "Router cannot be null");
        this.router.merge(router);
        return this;
    }

    @Override
    public App route(final String basePath, final Router router) {
        this.ensurePhase(LifecyclePhase.INIT);
        Objects.requireNonNull(basePath, "Base path cannot be null");
        Objects.requireNonNull(router, "Router cannot be null");
        this.router.merge(basePath, router);
        return this;
    }

    @Override
    public App route(final Supplier<Router> supplier) {
        this.ensurePhase(LifecyclePhase.INIT);
        Objects.requireNonNull(supplier, "Router supplier cannot be null");
        final Router r = supplier.get();
        if (r != null) {
            this.router.merge(r);
        }
        return this;
    }

    @Override
    public App route(final String basePath, final Supplier<Router> supplier) {
        this.ensurePhase(LifecyclePhase.INIT);
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
        this.ensurePhase(LifecyclePhase.INIT);
        Objects.requireNonNull(handler, "Error handler cannot be null");
        this.errorHandler = handler;
        return this;
    }

    // ========== Lifecycle Hooks ==========

    @Override
    public App onStart(final ThrowingRunnable action) {
        this.ensurePhase(LifecyclePhase.INIT);
        Objects.requireNonNull(action, "Start hook cannot be null");
        this.startHooks.add(action);
        return this;
    }

    @Override
    public App onReady(final Runnable action) {
        this.ensurePhase(LifecyclePhase.INIT);
        Objects.requireNonNull(action, "Ready hook cannot be null");
        this.readyHooks.add(action);
        return this;
    }

    @Override
    public App onShutdown(final ThrowingRunnable action) {
        this.ensurePhase(LifecyclePhase.INIT);
        Objects.requireNonNull(action, "Shutdown hook cannot be null");
        this.shutdownHooks.add(action);
        return this;
    }

    @Override
    public App onLifecycleError(final Consumer<Throwable> action) {
        this.ensurePhase(LifecyclePhase.INIT);
        Objects.requireNonNull(action, "Error hook cannot be null");
        this.errorHooks.add(action);
        return this;
    }

    // ========== Lifecycle Control ==========

    @Override
    public void listen(final int port) {
        this.listen(ServerConfig.builder()
                .host(this.serverConfig.host())
                .port(port)
                .maxRequestSize(this.serverConfig.maxRequestSize())
                .readTimeout(this.serverConfig.readTimeout())
                .writeTimeout(this.serverConfig.writeTimeout())
                .shutdownTimeout(this.serverConfig.shutdownTimeout())
                .drainTimeout(this.serverConfig.drainTimeout())
                .virtualThreads(this.serverConfig.virtualThreads())
                .build());
    }

    @Override
    public void listen(final String host, final int port) {
        this.listen(ServerConfig.builder()
                .host(host)
                .port(port)
                .maxRequestSize(this.serverConfig.maxRequestSize())
                .readTimeout(this.serverConfig.readTimeout())
                .writeTimeout(this.serverConfig.writeTimeout())
                .shutdownTimeout(this.serverConfig.shutdownTimeout())
                .drainTimeout(this.serverConfig.drainTimeout())
                .virtualThreads(this.serverConfig.virtualThreads())
                .build());
    }

    @Override
    public void listen(final ServerConfig config) {
        Objects.requireNonNull(config, "ServerConfig cannot be null");

        if (!this.phase.compareAndSet(LifecyclePhase.INIT, LifecyclePhase.STARTING)) {
            throw new IllegalStateException(
                    "Cannot start app in phase " + this.phase.get() +
                            ". App must be in INIT phase.");
        }

        this.serverConfig = config;
        DefaultApp.LOG.info("[Axiom] Starting application...");

        try {
            this.executeStartHooks();
            this.startServer(config);
            this.phase.set(LifecyclePhase.STARTED);
            DefaultApp.LOG.info("[Axiom] Application started on {}:{}", config.host(), this.activeServer.port());
            this.executeReadyHooks();
        } catch (final Exception e) {
            DefaultApp.LOG.error("Failed to start application: {}", e.getMessage(), e);
            this.transitionToError(e);
            throw new StartupException("Failed to start application", e);
        }
    }

    @Override
    public int port() {
        return this.activeServer != null ? this.activeServer.port() : -1;
    }

    @Override
    public void stop() {
        final LifecyclePhase currentPhase = this.phase.get();

        if (currentPhase == LifecyclePhase.STOPPED || currentPhase == LifecyclePhase.STOPPING) {
            return;
        }

        if (!this.phase.compareAndSet(currentPhase, LifecyclePhase.STOPPING)) {
            return;
        }

        DefaultApp.LOG.info("[Axiom] Stopping application...");
        final List<Throwable> failures = new ArrayList<>();

        try {
            if (this.activeServer != null) {
                this.activeServer.stop();
                this.activeServer = null;
            }
        } catch (final Exception e) {
            failures.add(e);
        }

        this.executeShutdownHooks(failures);

        this.phase.set(LifecyclePhase.STOPPED);

        if (failures.isEmpty()) {
            DefaultApp.LOG.info("[Axiom] Application stopped");
        } else {
            DefaultApp.LOG.warn("Shutdown completed with {} error(s)", failures.size());
            failures.forEach(f -> DefaultApp.LOG.warn("Shutdown hook failed", f));
        }
    }

    @Override
    public boolean isRunning() {
        return this.phase.get() == LifecyclePhase.STARTED &&
                this.activeServer != null &&
                this.activeServer.isRunning();
    }

    @Override
    public LifecyclePhase phase() {
        return this.phase.get();
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
    public Router router() {
        return this.router;
    }

    /**
     * Returns the configured error handler.
     *
     * @return the error handler
     */
    public ErrorHandler errorHandler() {
        return this.errorHandler;
    }

    /**
     * Returns the server configuration.
     *
     * @return the server config
     */
    public ServerConfig serverConfig() {
        return this.serverConfig;
    }

    // ========== Internal Lifecycle ==========

    private void executeStartHooks() throws Exception {
        for (final ThrowingRunnable hook : this.startHooks) {
            hook.run();
        }
    }

    private void executeReadyHooks() {
        for (final Runnable hook : this.readyHooks) {
            try {
                hook.run();
            } catch (final Exception e) {
                DefaultApp.LOG.warn("Ready hook failed: {}", e.getMessage(), e);
            }
        }
    }

    private void executeShutdownHooks(final List<Throwable> failures) {
        for (int i = this.shutdownHooks.size() - 1; i >= 0; i--) {
            try {
                this.shutdownHooks.get(i).run();
            } catch (final Exception e) {
                failures.add(e);
            }
        }
    }

    private void executeErrorHooks(final Throwable error) {
        for (final Consumer<Throwable> hook : this.errorHooks) {
            try {
                hook.accept(error);
            } catch (final Exception e) {
                DefaultApp.LOG.warn("Error hook failed: {}", e.getMessage(), e);
            }
        }
    }

    private void transitionToError(final Throwable error) {
        this.phase.set(LifecyclePhase.ERROR);
        this.executeErrorHooks(error);

        if (this.activeServer != null) {
            try {
                this.activeServer.stop();
            } catch (final Exception ignored) {
            }
            this.activeServer = null;
        }
    }

    private void startServer(final ServerConfig config) {
        final var server = Axiom.createServer();
        server.handler(this.buildHandler());
        server.start(config);
        this.activeServer = server;
    }

    // ========== Internal Handler Composition ==========

    private Handler composeHandler() {
        final Handler routingHandler = this::handleRequest;

        final Handler withErrorHandling = ctx -> {
            try {
                routingHandler.handle(ctx);
            } catch (final Exception e) {
                this.handleError(ctx, e);
            }
        };

        Handler result = withErrorHandling;
        for (int i = this.middlewares.size() - 1; i >= 0; i--) {
            result = this.middlewares.get(i).apply(result);
        }

        return result;
    }

    private void handleRequest(final Context ctx) throws Exception {
        final String method = ctx.method();
        final String path = ctx.path();

        final var matchOpt = this.router.match(method, path);

        if (matchOpt.isEmpty()) {
            if (this.router.hasRoute(path)) {
                final List<String> allowed = this.router.allowedMethods(path);
                throw new MethodNotAllowedException(method, path, allowed);
            }
            throw new RouteNotFoundException(method, path);
        }

        final RouteMatch match = matchOpt.get();

        if (ctx instanceof final io.axiom.core.context.DefaultContext defaultCtx) {
            defaultCtx.setPathParams(match.params());
        }

        match.route().handler().handle(ctx);
    }

    private void handleError(final Context ctx, final Exception e) {
        try {
            this.errorHandler.handle(ctx, e);
        } catch (final Exception errorHandlerException) {
            DefaultApp.LOG.error("Error handler failed while processing {}: {}",
                    e.getClass().getSimpleName(), errorHandlerException.getMessage(), errorHandlerException);
            try {
                ctx.status(500);
                ctx.text("Internal Server Error");
            } catch (final Exception ignored) {
            }
        }
    }

    private void ensurePhase(final LifecyclePhase required) {
        final LifecyclePhase current = this.phase.get();
        if (current != required) {
            throw new IllegalStateException(
                    "Cannot modify app in phase " + current +
                            ". App must be in " + required + " phase.");
        }
    }

    // ========== Default Error Handler ==========

    private static void defaultErrorHandler(final Context ctx, final Exception e) {
        switch (e) {
            case final RouteNotFoundException notFound -> {
                ctx.status(404);
                ctx.json(Map.of(
                        "error", "Not Found",
                        "method", notFound.method(),
                        "path", notFound.path()));
            }
            case final MethodNotAllowedException notAllowed -> {
                ctx.status(405);
                ctx.setHeader("Allow", String.join(", ", notAllowed.allowedMethods()));
                ctx.json(Map.of(
                        "error", "Method Not Allowed",
                        "path", notAllowed.path(),
                        "allowed", notAllowed.allowedMethods()));
            }
            case final BodyParseException _ -> {
                ctx.status(400);
                ctx.json(Map.of(
                        "error", "Bad Request",
                        "message", e.getMessage()));
            }
            case final ResponseCommittedException _ -> {
                DefaultApp.LOG.warn("Response already committed: {}", e.getMessage());
            }
            default -> {
                ctx.status(500);
                ctx.json(Map.of(
                        "error", "Internal Server Error",
                        "message", e.getMessage() != null ? e.getMessage() : "Unknown error"));
            }
        }
    }
}
