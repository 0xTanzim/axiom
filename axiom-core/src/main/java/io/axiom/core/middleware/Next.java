package io.axiom.core.middleware;

/**
 * Continuation function for middleware chains.
 *
 * <p>
 * Calling {@code run()} passes control to the next middleware
 * or handler in the chain. Not calling it short-circuits execution.
 *
 * <h2>Usage Patterns</h2>
 *
 * <h3>Pass Through</h3>
 * 
 * <pre>{@code
 * app.use((c, next) -> {
 *     // Do something before
 *     next.run(); // Continue chain
 *     // Do something after
 * });
 * }</pre>
 *
 * <h3>Short Circuit</h3>
 * 
 * <pre>{@code
 * app.use((c, next) -> {
 *     if (!isAuthorized(c)) {
 *         c.status(401);
 *         c.text("Unauthorized");
 *         return; // Don't call next - chain stops here
 *     }
 *     next.run();
 * });
 * }</pre>
 *
 * <h3>Error Handling</h3>
 * 
 * <pre>{@code
 * app.use((c, next) -> {
 *     try {
 *         next.run();
 *     } catch (Exception e) {
 *         c.status(500);
 *         c.json(Map.of("error", e.getMessage()));
 *     }
 * });
 * }</pre>
 *
 * @see MiddlewareHandler
 * @since 0.1.0
 */
@FunctionalInterface
public interface Next {

    /**
     * Continues execution to the next middleware or handler.
     *
     * <p>
     * If this is not called, the chain stops at the current middleware.
     *
     * @throws Exception if the next handler throws
     */
    void run() throws Exception;
}
