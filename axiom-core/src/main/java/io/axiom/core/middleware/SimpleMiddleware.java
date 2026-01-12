package io.axiom.core.middleware;

import io.axiom.core.context.*;

/**
 * Middleware interface with context-embedded next.
 *
 * <p>
 * This is an alternative middleware style where {@code next()} is called
 * on the context rather than passed as a parameter.
 *
 * <h2>Comparison</h2>
 *
 * <pre>{@code
 * // Style 1: Explicit next parameter (MiddlewareHandler)
 * app.use((ctx, next) -> {
 *     log(ctx.path());
 *     next.run();
 * });
 *
 * // Style 2: Context-embedded next (SimpleMiddleware)
 * app.use(ctx -> {
 *     log(ctx.path());
 *     ctx.next();
 * });
 * }</pre>
 *
 * <h2>When to Use</h2>
 * <ul>
 * <li>Use {@link MiddlewareHandler} when you need explicit control over next</li>
 * <li>Use {@code SimpleMiddleware} for cleaner single-parameter lambdas</li>
 * </ul>
 *
 * <h2>Authentication Example</h2>
 *
 * <pre>{@code
 * app.use(ctx -> {
 *     String token = ctx.header("Authorization");
 *     if (token == null) {
 *         ctx.status(401);
 *         ctx.json(Map.of("error", "Unauthorized"));
 *         return; // Short-circuit
 *     }
 *
 *     User user = authService.validate(token);
 *     ctx.set("user", user);
 *     ctx.next(); // Continue chain
 * });
 * }</pre>
 *
 * <h2>Timing Example</h2>
 *
 * <pre>{@code
 * app.use(ctx -> {
 *     long start = System.nanoTime();
 *     try {
 *         ctx.next();
 *     } finally {
 *         long elapsed = (System.nanoTime() - start) / 1_000_000;
 *         System.out.println(ctx.path() + " took " + elapsed + "ms");
 *     }
 * });
 * }</pre>
 *
 * @see MiddlewareHandler
 * @see Context#next()
 * @since 0.1.0
 */
@FunctionalInterface
public interface SimpleMiddleware {

    /**
     * Handles a request with context-embedded next.
     *
     * <p>
     * Call {@code ctx.next()} to continue to the next middleware/handler.
     * Not calling it short-circuits the chain.
     *
     * @param ctx the request/response context
     * @throws Exception if handling fails; propagates to error handler
     */
    void handle(Context ctx) throws Exception;
}
