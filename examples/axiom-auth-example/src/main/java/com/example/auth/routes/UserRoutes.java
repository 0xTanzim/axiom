package com.example.auth.routes;

import com.example.auth.domain.User;
import com.example.auth.middleware.AuthMiddleware;
import com.example.auth.service.AuthService;
import io.axiom.core.routing.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Protected user routes (require authentication).
 *
 * <p>All routes in this group require a valid JWT token.
 */
public class UserRoutes {

    private static final Logger LOG = LoggerFactory.getLogger(UserRoutes.class);

    private final AuthService authService;

    public UserRoutes(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Creates the router with user routes.
     *
     * <p>Note: AuthMiddleware must be applied at the App level
     * or via router grouping to protect these routes.
     */
    public Router router() {
        Router router = new Router();

        // GET /users/me - Get current user profile
        router.get("/me", ctx -> {
            Long userId = AuthMiddleware.getUserId(ctx);

            var user = authService.getUserById(userId);
            if (user.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of("error", "User not found"));
                return;
            }

            ctx.json(toUserResponse(user.get()));
        });

        // PUT /users/me - Update current user profile
        router.put("/me", ctx -> {
            Long userId = AuthMiddleware.getUserId(ctx);

            record UpdateRequest(String name) {}
            UpdateRequest request = ctx.body(UpdateRequest.class);

            if (request.name() == null || request.name().isBlank()) {
                ctx.status(400);
                ctx.json(Map.of("error", "Name is required"));
                return;
            }

            var updated = authService.updateUser(userId, request.name());
            if (updated.isEmpty()) {
                ctx.status(404);
                ctx.json(Map.of("error", "User not found"));
                return;
            }

            ctx.json(toUserResponse(updated.get()));
        });

        return router;
    }

    private Map<String, Object> toUserResponse(User user) {
        return Map.of(
                "id", user.id(),
                "email", user.email(),
                "name", user.name(),
                "createdAt", user.createdAt().toString()
        );
    }
}
