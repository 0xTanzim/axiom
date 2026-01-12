package io.axiom.core.handler;

import io.axiom.core.context.Context;

/**
 * Core request handler interface.
 *
 * <p>
 * This is the fundamental building block of Axiom. Every route endpoint,
 * middleware, and hook is ultimately a {@code Handler}.
 *
 * <h2>Design Rationale</h2>
 * <ul>
 * <li>Single method interface enables lambda usage</li>
 * <li>{@code void} return - response written via Context</li>
 * <li>{@code throws Exception} - checked exceptions allowed</li>
 * <li>No generics - keeps API simple and readable</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * 
 * <pre>{@code
 * // Lambda form (recommended)
 * Handler handler = c -> c.text("Hello");
 *
 * // Method reference
 * Handler handler = this::handleRequest;
 *
 * // Full form
 * Handler handler = new Handler() {
 *     @Override
 *     public void handle(Context c) throws Exception {
 *         c.json(Map.of("status", "ok"));
 *     }
 * };
 * }</pre>
 *
 * <h2>Exception Handling</h2>
 * <p>
 * Handlers may throw any exception. Uncaught exceptions propagate to
 * the global error handler configured on the application.
 *
 * @see io.axiom.core.context.Context
 * @since 0.1.0
 */
@FunctionalInterface
public interface Handler {

    /**
     * Handles an incoming request.
     *
     * <p>
     * The handler reads request data from the context and writes
     * response data back to it. The context manages all I/O.
     *
     * @param context the request/response context
     * @throws Exception if handling fails; propagates to error handler
     */
    void handle(Context context) throws Exception;
}
