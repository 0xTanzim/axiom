package io.axiom.core.middleware;

import java.util.Objects;

import io.axiom.core.handler.Handler;

/**
 * Adapts public {@link MiddlewareHandler} to internal {@link Middleware}.
 *
 * <p>
 * This adapter bridges the user-friendly Express-style middleware API
 * to the internal functional composition model.
 *
 * <h2>Transformation</h2>
 * 
 * <pre>
 * User writes:
 *   (c, next) -> { doSomething(); next.run(); }
 *
 * Becomes internally:
 *   nextHandler -> c -> { doSomething(); nextHandler.handle(c); }
 * </pre>
 *
 * @since 0.1.0
 */
public final class MiddlewareAdapter {

    private MiddlewareAdapter() {
        // Utility class
    }

    /**
     * Adapts a public middleware handler to internal middleware.
     *
     * @param handler the public middleware handler
     * @return internal middleware function
     */
    public static Middleware adapt(MiddlewareHandler handler) {
        Objects.requireNonNull(handler, "MiddlewareHandler cannot be null");

        return nextHandler -> context -> {
            Next next = () -> nextHandler.handle(context);
            handler.handle(context, next);
        };
    }

    /**
     * Creates a middleware from a simple before-handler.
     *
     * <p>
     * The handler runs before the chain continues.
     *
     * @param beforeHandler runs before the next handler
     * @return middleware that runs handler then continues
     */
    public static Middleware before(Handler beforeHandler) {
        Objects.requireNonNull(beforeHandler, "Before handler cannot be null");

        return nextHandler -> context -> {
            beforeHandler.handle(context);
            nextHandler.handle(context);
        };
    }

    /**
     * Creates a middleware from a simple after-handler.
     *
     * <p>
     * The handler runs after the chain completes.
     *
     * @param afterHandler runs after the next handler
     * @return middleware that continues then runs handler
     */
    public static Middleware after(Handler afterHandler) {
        Objects.requireNonNull(afterHandler, "After handler cannot be null");

        return nextHandler -> context -> {
            nextHandler.handle(context);
            afterHandler.handle(context);
        };
    }
}
