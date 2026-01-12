package io.axiom.core.app;

/**
 * Factory for creating Axiom application instances.
 *
 * <p>
 * Provides static factory methods for creating pre-configured
 * application instances.
 *
 * <h2>Basic Usage</h2>
 *
 * <pre>{@code
 * App app = Axiom.create();
 * app.use((ctx, next) -> { log(ctx.path()); next.run(); });
 * app.route(userRouter);
 * app.listen(8080);
 * }</pre>
 *
 * <h2>Alternative: Direct Instantiation</h2>
 *
 * <pre>{@code
 * App app = new DefaultApp();
 * }</pre>
 *
 * @since 0.1.0
 */
public final class Axiom {

    private Axiom() {
        // Factory class
    }

    /**
     * Creates a new Axiom application instance.
     *
     * <p>
     * Returns a {@link DefaultApp} which handles middleware and routing
     * but does not include an HTTP server. Use a runtime adapter for
     * serving requests.
     *
     * @return a new app instance
     */
    public static App create() {
        return new DefaultApp();
    }

    /**
     * Creates a new DefaultApp instance for direct access.
     *
     * <p>
     * Use this when you need access to DefaultApp-specific methods
     * like {@code buildHandler()} for testing.
     *
     * @return a new DefaultApp instance
     */
    public static DefaultApp createDefault() {
        return new DefaultApp();
    }
}
