package io.axiom.core.middleware;

import io.axiom.core.handler.Handler;

/**
 * Internal middleware representation using pure function composition.
 *
 * <p>
 * This interface is the functional core of the middleware system.
 * It wraps a handler and returns a new handler that may execute
 * code before/after the wrapped handler.
 *
 * <p>
 * <strong>Note:</strong> This is an internal API. Users interact with
 * {@link MiddlewareHandler} which provides a friendlier DX.
 *
 * <h2>Composition Model</h2>
 * 
 * <pre>{@code
 * // Each middleware wraps the next handler
 * Middleware logging = next -> c -> {
 *     System.out.println("Before: " + c.path());
 *     next.handle(c);
 *     System.out.println("After: " + c.path());
 * };
 *
 * // Composition: middleware1(middleware2(handler))
 * Handler composed = logging.apply(actualHandler);
 * }</pre>
 *
 * <h2>Pipeline Construction</h2>
 * <p>
 * At application startup, middleware is composed into a single handler:
 * 
 * <pre>{@code
 * Handler final = handler;
 * for (Middleware m : reverse(middlewares)) {
 *     final = m.apply(final);
 * }
 * }</pre>
 *
 * @see MiddlewareHandler
 * @see MiddlewareAdapter
 * @since 0.1.0
 */
@FunctionalInterface
public interface Middleware {

    /**
     * Wraps a handler with middleware logic.
     *
     * @param next the next handler in the chain
     * @return a new handler that includes middleware logic
     */
    Handler apply(Handler next);

    /**
     * Composes this middleware with another.
     *
     * <p>
     * The returned middleware first applies {@code this}, then {@code other}.
     *
     * @param other the middleware to apply after this one
     * @return composed middleware
     */
    default Middleware andThen(Middleware other) {
        return next -> this.apply(other.apply(next));
    }

    /**
     * Creates a middleware that does nothing (passes through).
     *
     * @return identity middleware
     */
    static Middleware identity() {
        return next -> next;
    }

    /**
     * Composes multiple middleware into a single middleware.
     *
     * <p>
     * Middleware are applied in order: first middleware is outermost.
     *
     * @param middlewares the middleware to compose
     * @return single composed middleware
     */
    static Middleware compose(Middleware... middlewares) {
        Middleware result = identity();
        for (Middleware m : middlewares) {
            result = result.andThen(m);
        }
        return result;
    }
}
