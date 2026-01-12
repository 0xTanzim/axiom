package io.axiom.core.error;

import io.axiom.core.context.Context;

/**
 * Handler for uncaught exceptions during request processing.
 *
 * <p>
 * Error handlers receive exceptions that escape from handlers
 * and middleware. They are responsible for generating appropriate
 * error responses.
 *
 * <h2>Usage</h2>
 * 
 * <pre>{@code
 * app.onError((c, e) -> {
 *     if (e instanceof RouteNotFoundException) {
 *         c.status(404);
 *         c.json(Map.of("error", "Not Found"));
 *     } else if (e instanceof BodyParseException) {
 *         c.status(400);
 *         c.json(Map.of("error", "Invalid Request"));
 *     } else {
 *         c.status(500);
 *         c.json(Map.of("error", "Internal Server Error"));
 *         log.error("Unhandled exception", e);
 *     }
 * });
 * }</pre>
 *
 * <h2>Default Behavior</h2>
 * <p>
 * If no error handler is registered, the framework provides a
 * default handler that:
 * <ul>
 * <li>Returns 404 for RouteNotFoundException</li>
 * <li>Returns 405 for MethodNotAllowedException</li>
 * <li>Returns 400 for BodyParseException</li>
 * <li>Returns 500 for all other exceptions</li>
 * </ul>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface ErrorHandler {

    /**
     * Handles an exception that occurred during request processing.
     *
     * <p>
     * The context may already have partial response data written.
     * The handler should check if the response is committed before
     * attempting to write.
     *
     * @param context   the request/response context
     * @param exception the exception that was thrown
     */
    void handle(Context context, Exception exception);
}
