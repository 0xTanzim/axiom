package io.axiom.core.app;

import java.util.*;

import org.slf4j.*;

import io.axiom.core.routing.*;
import io.axiom.core.server.*;

/**
 * Factory for creating Axiom application instances.
 *
 * <p>Follows RFC-0001 patterns exactly.
 *
 * <h2>Style 1: Simple Router (For Small Apps)</h2>
 * <pre>{@code
 * Router router = new Router();
 * router.get("/health", c -> c.text("OK"));
 * Axiom.start(router, 8080);
 * }</pre>
 *
 * <h2>Style 2: Full Control (RFC-0001 style)</h2>
 * <pre>{@code
 * App app = Axiom.create();
 * app.use((ctx, next) -> { log(ctx.path()); next.run(); });
 * app.route("/users", UserRoutes::router);
 * app.onError((ctx, e) -> { ... });
 * app.listen(8080);
 * }</pre>
 *
 * <h2>Style 3: Auto-Discovery with DI (Recommended for larger apps)</h2>
 * <pre>{@code
 * // Use AxiomApplication from io.axiom.di package
 * AxiomApplication.start(Application.class, 8080);
 * }</pre>
 *
 * @see io.axiom.di.AxiomApplication
 * @since 0.1.0
 */
public final class Axiom {

    private static final Logger LOG = LoggerFactory.getLogger(Axiom.class);

    private static volatile ServerFactory cachedFactory;

    private Axiom() {
        // Factory class
    }

    // ========== Simple Router Start Methods ==========

    /**
     * Starts a server with a single router.
     *
     * <p>This is the simplest way to start an Axiom application:
     * <pre>{@code
     * Router router = new Router();
     * router.get("/health", c -> c.text("OK"));
     * Axiom.start(router, 8080);
     * }</pre>
     *
     * @param router the router with all routes
     * @param port   the port to listen on
     */
    public static void start(final Router router, final int port) {
        final App app = Axiom.create();
        app.route(router);
        app.listen(port);
    }

    /**
     * Starts a server with a single router on specified host.
     *
     * @param router the router with all routes
     * @param host   the host to bind to
     * @param port   the port to listen on
     */
    public static void start(final Router router, final String host, final int port) {
        final App app = Axiom.create();
        app.route(router);
        app.listen(host, port);
    }

    // ========== Full Control Methods ==========

    /**
     * Creates a new App instance (RFC-0001 pattern).
     *
     * <pre>{@code
     * App app = Axiom.create();
     * app.use((ctx, next) -> { ... });
     * app.route("/users", UserRoutes::router);
     * app.listen(8080);
     * }</pre>
     *
     * @return a new App instance
     */
    public static App create() {
        return new DefaultApp();
    }

    /**
     * Creates a new DefaultApp for testing or direct access.
     *
     * <p>
     * Use this when you need access to DefaultApp-specific methods
     * like {@code buildHandler()} for unit testing.
     *
     * @return a new DefaultApp instance
     */
    public static DefaultApp createDefault() {
        return new DefaultApp();
    }

    // ========== Server Factory Methods ==========

    /**
     * Discovers and returns the best available server factory.
     *
     * <p>
     * Factories are discovered via {@link ServiceLoader} and the one
     * with highest priority is returned. Results are cached.
     *
     * @return the server factory with highest priority
     * @throws IllegalStateException if no server runtime is available
     */
    public static ServerFactory serverFactory() {
        if (Axiom.cachedFactory != null) {
            return Axiom.cachedFactory;
        }

        synchronized (Axiom.class) {
            if (Axiom.cachedFactory != null) {
                return Axiom.cachedFactory;
            }

            Axiom.cachedFactory = ServiceLoader.load(ServerFactory.class)
                    .stream()
                    .map(ServiceLoader.Provider::get)
                    .max(Comparator.comparingInt(ServerFactory::priority))
                    .orElseThrow(() -> new IllegalStateException(
                            "No Axiom server runtime found. " +
                            "Add 'io.axiom:axiom-server' to your dependencies."));

            return Axiom.cachedFactory;
        }
    }

    /**
     * Creates a new server instance using the discovered factory.
     *
     * @return a new server ready for configuration
     * @throws IllegalStateException if no server runtime is available
     */
    public static Server createServer() {
        return Axiom.serverFactory().create();
    }

    /**
     * Returns the name of the discovered server runtime.
     *
     * @return server name or "none" if not available
     */
    public static String serverName() {
        try {
            return Axiom.serverFactory().name();
        } catch (final IllegalStateException e) {
            return "none";
        }
    }
}
