package io.axiom.examples.auth.middleware;

import io.axiom.core.context.*;
import io.axiom.core.middleware.*;
import io.axiom.examples.auth.domain.*;

import java.util.*;

/**
 * Authentication middleware example - validates JWT/Bearer tokens.
 *
 * <h2>Usage in Auto-Discovery Mode</h2>
 * <pre>{@code
 * @Middleware(order = 10)  // After logging and CORS
 * public class AuthMiddleware { ... }
 * }</pre>
 *
 * <h2>Usage in Manual Mode</h2>
 * <pre>{@code
 * // Apply to all routes
 * app.use(new AuthMiddleware(tokenService));
 *
 * // Or apply to specific routes only
 * Router protectedRouter = new Router();
 * protectedRouter.use(new AuthMiddleware(tokenService));
 * protectedRouter.get("/profile", ctx -> ...);
 * app.route("/api", protectedRouter);
 * }</pre>
 *
 * <h2>Inline Version</h2>
 * <pre>{@code
 * app.use((ctx, next) -> {
 *     String auth = ctx.header("Authorization");
 *     if (auth == null || !auth.startsWith("Bearer ")) {
 *         ctx.status(401);
 *         ctx.json(Map.of("error", "Unauthorized"));
 *         return;
 *     }
 *     String token = auth.substring(7);
 *     // validate token...
 *     ctx.set("userId", userId);
 *     next.run();
 * });
 * }</pre>
 */
@io.axiom.di.Middleware(order = 10)
public class AuthMiddleware implements MiddlewareHandler {

    // Paths that don't require authentication
    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/health",
        "/auth/login",
        "/auth/register"
    );

    @Override
    public void handle(Context ctx, Next next) throws Exception {
        String path = ctx.path();

        // Skip auth for public paths
        if (isPublicPath(path)) {
            next.run();
            return;
        }

        // Check Authorization header
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            unauthorized(ctx, "Missing or invalid Authorization header");
            return;
        }

        // Extract and validate token
        String token = authHeader.substring(7);
        if (!isValidToken(token)) {
            unauthorized(ctx, "Invalid or expired token");
            return;
        }

        // Store user info in context for handlers
        ctx.set("token", token);
        ctx.set("userId", extractUserId(token));

        next.run();
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean isValidToken(String token) {
        // In real app: verify JWT signature, check expiry, etc.
        return token != null && token.length() > 10;
    }

    private String extractUserId(String token) {
        // In real app: decode JWT and extract user ID
        return token.split("-")[token.split("-").length - 1];
    }

    private void unauthorized(Context ctx, String message) {
        ctx.status(401);
        ctx.json(ErrorResponse.unauthorized(message));
    }
}
