package io.axiom.examples.auth.middleware;

import io.axiom.core.context.*;
import io.axiom.core.middleware.*;

/**
 * CORS middleware example - enables cross-origin requests.
 *
 * <h2>Usage in Auto-Discovery Mode</h2>
 * <pre>{@code
 * @Middleware(order = 2)
 * public class CorsMiddleware { ... }
 * }</pre>
 *
 * <h2>Usage in Manual Mode</h2>
 * <pre>{@code
 * app.use(new CorsMiddleware("*"));
 * // or inline:
 * app.use((ctx, next) -> {
 *     ctx.header("Access-Control-Allow-Origin", "*");
 *     ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
 *     ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
 *
 *     if ("OPTIONS".equals(ctx.method())) {
 *         ctx.status(204);
 *         ctx.text("");
 *         return;
 *     }
 *     next.run();
 * });
 * }</pre>
 */
@io.axiom.di.Middleware(order = 2)
public class CorsMiddleware implements MiddlewareHandler {

    private final String allowedOrigins;
    private final String allowedMethods;
    private final String allowedHeaders;

    public CorsMiddleware() {
        this("*", "GET, POST, PUT, DELETE, OPTIONS", "Content-Type, Authorization");
    }

    public CorsMiddleware(String allowedOrigins) {
        this(allowedOrigins, "GET, POST, PUT, DELETE, OPTIONS", "Content-Type, Authorization");
    }

    public CorsMiddleware(String allowedOrigins, String allowedMethods, String allowedHeaders) {
        this.allowedOrigins = allowedOrigins;
        this.allowedMethods = allowedMethods;
        this.allowedHeaders = allowedHeaders;
    }

    @Override
    public void handle(Context ctx, Next next) throws Exception {
        // Set CORS headers
        ctx.header("Access-Control-Allow-Origin", allowedOrigins);
        ctx.header("Access-Control-Allow-Methods", allowedMethods);
        ctx.header("Access-Control-Allow-Headers", allowedHeaders);

        // Handle preflight requests
        if ("OPTIONS".equals(ctx.method())) {
            ctx.status(204);
            ctx.text("");
            return;
        }

        next.run();
    }
}
