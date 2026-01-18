package io.axiom.examples.auth.routes;

import io.axiom.core.routing.Router;
import io.axiom.di.Routes;
import io.axiom.examples.auth.domain.ErrorResponse;
import io.axiom.examples.auth.service.UserService;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * User routes: list, get, delete.
 *
 * <p>Demonstrates data retrieval patterns:
 * - Path parameters with c.param("id")
 * - Query parameters with c.query("key")
 * - List and single-item responses
 */
@Routes("/users")
public class UserRoutes {

    private final UserService userService;

    @Inject
    public UserRoutes(UserService userService) {
        this.userService = userService;
    }

    public Router router() {
        var router = new Router();

        // GET /users - List all users
        router.get("/", ctx -> {
            var users = userService.findAll();
            ctx.json(Map.of(
                "count", users.size(),
                "users", users
            ));
        });

        // GET /users/:id - Get user by ID
        router.get("/:id", ctx -> {
            var idParam = ctx.param("id");

            long id;
            try {
                id = Long.parseLong(idParam);
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(ErrorResponse.badRequest("Invalid user ID format"));
                return;
            }

            var userOpt = userService.findById(id);
            if (userOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(ErrorResponse.notFound("User not found"));
                return;
            }

            ctx.json(userOpt.get());
        });

        // GET /users/by-username/:username - Get user by username
        router.get("/by-username/:username", ctx -> {
            var username = ctx.param("username");

            var userOpt = userService.findByUsername(username);
            if (userOpt.isEmpty()) {
                ctx.status(404);
                ctx.json(ErrorResponse.notFound("User not found"));
                return;
            }

            ctx.json(userOpt.get());
        });

        // DELETE /users/:id - Delete user
        router.delete("/:id", ctx -> {
            var idParam = ctx.param("id");

            long id;
            try {
                id = Long.parseLong(idParam);
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(ErrorResponse.badRequest("Invalid user ID format"));
                return;
            }

            if (userService.deleteById(id)) {
                ctx.status(204);
                ctx.text("");
            } else {
                ctx.status(404);
                ctx.json(ErrorResponse.notFound("User not found"));
            }
        });

        // GET /users/stats - Get user statistics
        router.get("/stats", ctx -> {
            ctx.json(Map.of(
                "totalUsers", userService.count()
            ));
        });

        return router;
    }
}
