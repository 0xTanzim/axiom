package io.axiom.examples.auth;

import io.axiom.core.app.*;
import io.axiom.core.routing.*;
import io.axiom.examples.auth.repository.*;
import io.axiom.examples.auth.service.*;

/**
 * MANUAL/EXPLICIT approach - full control, no auto-discovery.
 *
 * <p>This is RFC-0002 style: explicit routing, explicit wiring.
 * Use this when you want complete control.
 *
 * <h2>Run this</h2>
 * <pre>
 * mvn compile exec:java -Dexec.mainClass=io.axiom.examples.auth.ManualApplication
 * </pre>
 */
public class ManualApplication {

    public static void main(String[] args) {
        // Manual DI wiring
        UserRepository userRepository = new UserRepository();
        AuthService authService = new AuthService(userRepository);
        UserService userService = new UserService(userRepository);

        // Create app
        App app = Axiom.create();

        // Middleware (pick your style)
        app.use((ctx, next) -> {
            System.out.println(ctx.method() + " " + ctx.path());
            next.run();
        });

        // Health routes
        Router healthRouter = new Router();
        healthRouter.get("/", ctx -> ctx.json(new HealthResponse("healthy", System.currentTimeMillis())));

        // User routes
        Router userRouter = new Router();
        userRouter.get("/", ctx -> {
            var users = userRepository.findAll().stream()
                .map(u -> u.toResponse())
                .toList();
            ctx.json(new UsersResponse(users, users.size()));
        });
        userRouter.get("/:id", ctx -> {
            long id = Long.parseLong(ctx.param("id"));
            userRepository.findById(id).ifPresentOrElse(
                user -> ctx.json(user.toResponse()),
                () -> {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("User not found"));
                }
            );
        });

        // Mount routes
        app.route("/health", healthRouter);
        app.route("/users", userRouter);

        // Start server
        app.listen(8080);
    }

    // Inline DTOs for this demo
    record HealthResponse(String status, long timestamp) {}
    record UsersResponse(java.util.List<?> users, int count) {}
    record ErrorResponse(String error) {}
}
