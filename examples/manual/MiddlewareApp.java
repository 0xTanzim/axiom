package playground.manual;

import io.axiom.core.app.*;
import io.axiom.core.routing.*;

import java.util.*;

/**
 * Demonstrates all middleware patterns in Axiom.
 *
 * Middleware Styles:
 * 1. Explicit next: (ctx, next) -> { next.run(); }
 * 2. Context next:  ctx -> { ctx.next(); }
 *
 * Concepts shown:
 * - Global middleware (app.use)
 * - Route-specific middleware (router.use)
 * - Before/after hooks
 * - Short-circuit (early return)
 * - Request timing
 * - Authentication pattern
 */
public class MiddlewareApp {

    public static void main(String[] args) {
        App app = Axiom.create();

        // ===========================================
        // GLOBAL MIDDLEWARE (runs for all routes)
        // ===========================================

        // Style 1: Explicit next parameter
        app.use((ctx, next) -> {
            long start = System.currentTimeMillis();
            System.out.println("→ " + ctx.method() + " " + ctx.path());

            next.run();  // Continue to next middleware/handler

            long duration = System.currentTimeMillis() - start;
            System.out.println("← " + ctx.method() + " " + ctx.path() + " (" + duration + "ms)");
        });

        // Style 2: Context-embedded next (equivalent behavior)
        app.use(ctx -> {
            ctx.setHeader("X-Powered-By", "Axiom");
            ctx.next();  // Same as next.run() in style 1
        });

        // ===========================================
        // BEFORE/AFTER HOOKS
        // ===========================================

        app.before(ctx -> {
            System.out.println("  [before] Processing: " + ctx.path());
        });

        app.after(ctx -> {
            System.out.println("  [after] Completed: " + ctx.path());
        });

        // ===========================================
        // PUBLIC ROUTES (no auth required)
        // ===========================================

        Router publicRouter = new Router();

        publicRouter.get("/health", ctx -> {
            ctx.json(Map.of("status", "healthy"));
        });

        publicRouter.get("/login", ctx -> {
            ctx.json(Map.of("message", "Login endpoint"));
        });

        // ===========================================
        // PROTECTED ROUTES (with auth middleware)
        // ===========================================

        Router protectedRouter = new Router();

        // Router-specific middleware - only applies to this router
        protectedRouter.use((ctx, next) -> {
            String authHeader = ctx.header("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                // Short-circuit: don't call next.run()
                ctx.status(401);
                ctx.json(Map.of("error", "Missing or invalid Authorization header"));
                return;  // Early return, stops here
            }

            String token = authHeader.substring(7);

            // Validate token (simplified)
            if (token.length() < 10) {
                ctx.status(401);
                ctx.json(Map.of("error", "Invalid token"));
                return;
            }

            // Store user info for handlers
            ctx.set("userId", extractUserId(token));
            ctx.set("token", token);

            next.run();  // Continue to handler
        });

        protectedRouter.get("/profile", ctx -> {
            String userId = ctx.get("userId", String.class).orElse("unknown");
            ctx.json(Map.of(
                "userId", userId,
                "message", "This is your protected profile"
            ));
        });

        protectedRouter.get("/settings", ctx -> {
            ctx.json(Map.of("theme", "dark", "language", "en"));
        });

        // ===========================================
        // ADMIN ROUTES (stricter middleware)
        // ===========================================

        Router adminRouter = new Router();

        // Admin check middleware
        adminRouter.use((ctx, next) -> {
            String role = ctx.header("X-User-Role");

            if (!"admin".equals(role)) {
                ctx.status(403);
                ctx.json(Map.of("error", "Admin access required"));
                return;
            }

            next.run();
        });

        adminRouter.get("/stats", ctx -> {
            ctx.json(Map.of(
                "users", 1234,
                "requests", 56789,
                "uptime", "99.9%"
            ));
        });

        adminRouter.delete("/users/:id", ctx -> {
            String id = ctx.paramOrThrow("id");
            ctx.json(Map.of("deleted", id));
        });

        // ===========================================
        // MOUNT ROUTERS
        // ===========================================

        app.route(publicRouter);                    // No prefix
        app.route("/api", protectedRouter);         // /api/profile, /api/settings
        app.route("/admin", adminRouter);           // /admin/stats, /admin/users/:id

        // ===========================================
        // ERROR HANDLER
        // ===========================================

        app.onError((ctx, error) -> {
            System.err.println("Error: " + error.getMessage());
            error.printStackTrace();

            ctx.status(500);
            ctx.json(Map.of(
                "error", "Internal server error",
                "message", error.getMessage()
            ));
        });

        // Start
        System.out.println("Starting MiddlewareApp at http://localhost:8080");
        System.out.println("Test commands:");
        System.out.println("  curl http://localhost:8080/health");
        System.out.println("  curl http://localhost:8080/api/profile");
        System.out.println("  curl http://localhost:8080/api/profile -H 'Authorization: Bearer demo-token-123'");
        System.out.println("  curl http://localhost:8080/admin/stats -H 'X-User-Role: admin'");

        app.listen(8080);
    }

    private static String extractUserId(String token) {
        // In real app: decode JWT
        return "user-" + token.substring(0, Math.min(8, token.length()));
    }
}
