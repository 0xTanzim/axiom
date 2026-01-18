package com.example.auth.middleware;

import com.example.auth.service.JwtService;
import io.axiom.core.context.Context;
import io.axiom.core.middleware.MiddlewareHandler;
import io.axiom.core.middleware.Next;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

/**
 * JWT authentication middleware.
 *
 * <p>Extracts and validates the Bearer token from the Authorization header.
 * If valid, stores the user ID in the context as an attribute.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Apply to protected routes
 * router.group("/users", users -> {
 *     users.use(new AuthMiddleware(jwtService));
 *     users.get("/me", ctx -> {
 *         Long userId = ctx.attribute("userId");
 *         // ...
 *     });
 * });
 * }</pre>
 */
public class AuthMiddleware implements MiddlewareHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AuthMiddleware.class);
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    public static final String USER_ID_ATTR = "userId";

    private final JwtService jwtService;

    public AuthMiddleware(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void handle(Context ctx, Next next) throws Exception {
        String authHeader = ctx.header(AUTH_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            LOG.debug("Missing or invalid Authorization header");
            unauthorized(ctx, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        Optional<Long> userIdOpt = jwtService.validateToken(token);

        if (userIdOpt.isEmpty()) {
            LOG.debug("Invalid or expired token");
            unauthorized(ctx, "Invalid or expired token");
            return;
        }

        Long userId = userIdOpt.get();
        ctx.set(USER_ID_ATTR, userId);
        LOG.debug("Authenticated user: {}", userId);

        next.run();
    }

    private void unauthorized(Context ctx, String message) {
        ctx.status(401);
        ctx.json(Map.of(
                "error", "Unauthorized",
                "message", message
        ));
    }

    /**
     * Helper to extract user ID from context (set by this middleware).
     */
    public static Long getUserId(Context ctx) {
        Long userId = ctx.get(USER_ID_ATTR, Long.class).orElse(null);
        if (userId == null) {
            throw new IllegalStateException("User ID not found in context. Is AuthMiddleware applied?");
        }
        return userId;
    }
}
