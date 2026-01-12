package io.axiom.core.middleware;

import io.axiom.core.context.Context;

/**
 * Public middleware interface with Express/Hono-style DX.
 *
 * <p>
 * This is the primary interface for writing middleware in Axiom.
 * It provides a familiar, intuitive API that matches modern web frameworks.
 *
 * <h2>Basic Usage</h2>
 * 
 * <pre>{@code
 * app.use((c, next) -> {
 *     System.out.println("Request: " + c.method() + " " + c.path());
 *     next.run();
 *     System.out.println("Response sent");
 * });
 * }</pre>
 *
 * <h2>Authentication Guard</h2>
 * 
 * <pre>{@code
 * app.use((c, next) -> {
 *     String token = c.header("Authorization");
 *     if (token == null) {
 *         c.status(401);
 *         c.json(Map.of("error", "Unauthorized"));
 *         return; // Short-circuit - don't call next
 *     }
 *
 *     User user = authService.validate(token);
 *     c.set("user", user);
 *     next.run();
 * });
 * }</pre>
 *
 * <h2>Timing Middleware</h2>
 * 
 * <pre>{@code
 * app.use((c, next) -> {
 *     long start = System.nanoTime();
 *     try {
 *         next.run();
 *     } finally {
 *         long elapsed = System.nanoTime() - start;
 *         System.out.println(c.path() + " took " + elapsed / 1_000_000 + "ms");
 *     }
 * });
 * }</pre>
 *
 * <h2>Control Flow</h2>
 * <ul>
 * <li>Call {@code next.run()} to continue to the next middleware/handler</li>
 * <li>Return without calling next to short-circuit the chain</li>
 * <li>Exceptions propagate to the global error handler</li>
 * </ul>
 *
 * @see Next
 * @see Middleware
 * @since 0.1.0
 */
@FunctionalInterface
public interface MiddlewareHandler {

    /**
     * Handles a request with access to the next handler.
     *
     * @param context the request/response context
     * @param next    continues execution to the next middleware/handler
     * @throws Exception if handling fails; propagates to error handler
     */
    void handle(Context context, Next next) throws Exception;
}
