package io.axiom.core.middleware;

import java.util.*;

import io.axiom.core.context.*;
import io.axiom.core.handler.*;

/**
 * Adapts public middleware interfaces to internal {@link MiddlewareFunction}.
 *
 * <p>
 * This adapter bridges the user-friendly middleware APIs to the internal
 * functional composition model. Supports both middleware styles:
 *
 * <h2>Style 1: Explicit Next (MiddlewareHandler)</h2>
 *
 * <pre>{@code
 * app.use((ctx, next) -> {
 *     doSomething();
 *     next.run();
 * });
 * }</pre>
 *
 * <h2>Style 2: Context-Embedded Next (SimpleMiddleware)</h2>
 *
 * <pre>{@code
 * app.use(ctx -> {
 *     doSomething();
 *     ctx.next();
 * });
 * }</pre>
 *
 * <h2>Internal Transformation</h2>
 *
 * <pre>
 * User writes:     (ctx, next) -> { ... next.run(); }
 * Becomes:         nextHandler -> ctx -> { ... nextHandler.handle(ctx); }
 * </pre>
 *
 * @since 0.1.0
 */
public final class MiddlewareAdapter {

    private MiddlewareAdapter() {
        // Utility class
    }

    /**
     * Adapts explicit-next style middleware.
     *
     * @param handler the middleware with (ctx, next) signature
     * @return internal middleware function
     * @throws NullPointerException if handler is null
     */
    public static MiddlewareFunction adapt(final MiddlewareHandler handler) {
        Objects.requireNonNull(handler, "MiddlewareHandler cannot be null");

        return nextHandler -> context -> {
            final Next next = () -> nextHandler.handle(context);
            handler.handle(context, next);
        };
    }

    /**
     * Adapts context-embedded-next style middleware.
     *
     * <p>
     * This enables the {@code ctx.next()} pattern by injecting
     * the next handler into the context before execution.
     *
     * @param middleware the middleware with ctx.next() style
     * @return internal middleware function
     * @throws NullPointerException if middleware is null
     */
    public static MiddlewareFunction adapt(final SimpleMiddleware middleware) {
        Objects.requireNonNull(middleware, "SimpleMiddleware cannot be null");

        return nextHandler -> context -> {
            final Next next = () -> nextHandler.handle(context);

            // Inject next into context if it's DefaultContext
            if (context instanceof final DefaultContext defaultCtx) {
                defaultCtx.setNext(next);
            }

            middleware.handle(context);
        };
    }

    /**
     * Creates a middleware from a before-handler hook.
     *
     * <p>
     * Before hooks run before the main handler and always continue.
     * They cannot short-circuit the request.
     *
     * @param beforeHandler runs before the next handler
     * @return middleware that runs handler then continues
     * @throws NullPointerException if beforeHandler is null
     */
    public static MiddlewareFunction before(final Handler beforeHandler) {
        Objects.requireNonNull(beforeHandler, "Before handler cannot be null");

        return nextHandler -> context -> {
            beforeHandler.handle(context);
            nextHandler.handle(context);
        };
    }

    /**
     * Creates a middleware from an after-handler hook.
     *
     * <p>
     * After hooks run after the main handler completes successfully.
     * Exceptions from the main handler bypass the after hook.
     *
     * @param afterHandler runs after the next handler
     * @return middleware that continues then runs handler
     * @throws NullPointerException if afterHandler is null
     */
    public static MiddlewareFunction after(final Handler afterHandler) {
        Objects.requireNonNull(afterHandler, "After handler cannot be null");

        return nextHandler -> context -> {
            nextHandler.handle(context);
            afterHandler.handle(context);
        };
    }

    /**
     * Creates a middleware from an after-handler that always runs.
     *
     * <p>
     * Unlike {@link #after(Handler)}, this runs even if an exception occurs.
     * Useful for cleanup, logging, or releasing resources.
     *
     * @param finallyHandler runs after the next handler (always)
     * @return middleware that continues then runs handler in finally block
     * @throws NullPointerException if finallyHandler is null
     */
    public static MiddlewareFunction afterAlways(final Handler finallyHandler) {
        Objects.requireNonNull(finallyHandler, "Finally handler cannot be null");

        return nextHandler -> context -> {
            try {
                nextHandler.handle(context);
            } finally {
                finallyHandler.handle(context);
            }
        };
    }

    /**
     * Creates a conditional middleware that only applies to matching paths.
     *
     * @param pathPrefix the path prefix to match (e.g., "/api")
     * @param middleware the middleware to apply if path matches
     * @return middleware that conditionally executes
     * @throws NullPointerException if any argument is null
     */
    public static MiddlewareFunction forPath(final String pathPrefix, final MiddlewareFunction middleware) {
        Objects.requireNonNull(pathPrefix, "Path prefix cannot be null");
        Objects.requireNonNull(middleware, "Middleware cannot be null");

        return nextHandler -> {
            final Handler wrapped = middleware.apply(nextHandler);
            return context -> {
                if (context.path().startsWith(pathPrefix)) {
                    wrapped.handle(context);
                } else {
                    nextHandler.handle(context);
                }
            };
        };
    }
}
