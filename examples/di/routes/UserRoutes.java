package playground.di.routes;

import io.axiom.core.routing.Router;
import io.axiom.di.Routes;
import jakarta.inject.Inject;
import playground.di.service.UserService;

import java.util.Map;

/**
 * User routes: list, get by ID.
 *
 * @Routes("/users") — auto-mounted at /users
 *
 * Endpoints:
 *   GET /users      — List all users
 *   GET /users/:id  — Get user by ID
 */
@Routes("/users")
public class UserRoutes {

    private final UserService userService;

    @Inject
    public UserRoutes(UserService userService) {
        this.userService = userService;
    }

    public Router router() {
        Router r = new Router();

        // GET /users — List all users
        r.get("/", ctx -> {
            var users = userService.findAll();
            ctx.json(Map.of(
                "users", users,
                "count", users.size()
            ));
        });

        // GET /users/:id — Get user by ID
        r.get("/:id", ctx -> {
            long id;
            try {
                id = Long.parseLong(ctx.paramOrThrow("id"));
            } catch (NumberFormatException e) {
                ctx.status(400);
                ctx.json(Map.of("error", "Invalid user ID"));
                return;
            }

            userService.findById(id).ifPresentOrElse(
                user -> ctx.json(user),
                () -> {
                    ctx.status(404);
                    ctx.json(Map.of("error", "User not found"));
                }
            );
        });

        return r;
    }
}
