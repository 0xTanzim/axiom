package io.axiom.core.app;

import java.util.*;

import io.axiom.core.server.*;

/**
 * Factory for creating Axiom application instances.
 *
 * <p>
 * Provides static factory methods for creating pre-configured
 * application instances with automatic server runtime discovery.
 *
 * <h2>Basic Usage</h2>
 *
 * <pre>{@code
 * App app = Axiom.create();
 * app.use((ctx, next) -> { log(ctx.path()); next.run(); });
 * app.route(userRouter);
 * app.listen(8080);  // Auto-discovers server runtime!
 * }</pre>
 *
 * <h2>How It Works</h2>
 *
 * <p>
 * Axiom uses Java's {@link ServiceLoader} to discover server implementations
 * at runtime. When you call {@code app.listen()}, the factory finds the
 * best available server implementation automatically.
 *
 * <h2>Available Runtimes</h2>
 *
 * <ul>
 *   <li>axiom-server — JDK HttpServer with virtual threads (recommended)</li>
 *   <li>axiom-server-netty — Netty for high-performance needs (optional)</li>
 * </ul>
 *
 * @since 0.1.0
 */
public final class Axiom {

    private static volatile ServerFactory cachedFactory;

    private Axiom() {
        // Factory class
    }

    /**
     * Creates a new Axiom application instance.
     *
     * <p>
     * The returned app auto-discovers the server runtime when
     * {@code listen()} is called. Just add axiom-server to your
     * dependencies and it works!
     *
     * @return a new app instance ready for configuration
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
