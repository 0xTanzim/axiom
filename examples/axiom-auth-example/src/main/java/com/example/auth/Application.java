package com.example.auth;

import com.example.auth.config.AppConfig;
import io.axiom.config.AxiomConfig;
import io.axiom.core.app.App;
import io.axiom.core.app.Axiom;
import io.axiom.persistence.AxiomPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;

/**
 * Main application entry point.
 *
 * <p>Demonstrates a complete Axiom application with:
 * <ul>
 *   <li>Type-safe configuration</li>
 *   <li>Database persistence</li>
 *   <li>JWT authentication</li>
 *   <li>Clean dependency injection via Services container</li>
 *   <li>Route organization</li>
 *   <li>Lifecycle hooks</li>
 * </ul>
 *
 * <h2>Running</h2>
 * <pre>
 * # Development
 * mvn compile exec:java
 *
 * # Production
 * java --enable-preview -jar target/axiom-auth-example.jar
 * </pre>
 */
public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        // ========== 1. Load Configuration ==========
        AppConfig appConfig = AxiomConfig.builder()
                .withMapping(AppConfig.class)
                .withDotEnvFile(Path.of(".env"))
                .build()
                .bind(AppConfig.class);

        LOG.info("Loaded configuration: port={}", appConfig.server().port());

        // ========== 2. Initialize Database (Global Singleton) ==========
        AxiomPersistence.start(b -> b
                .config(io.axiom.persistence.config.PersistenceConfig.builder()
                        .url(appConfig.database().url())
                        .username(appConfig.database().username())
                        .password(appConfig.database().password())
                        .build()));

        // ========== 3. Create Services (All DI in One Place!) ==========
        Services services = new Services(appConfig);

        // ========== 4. Build Application ==========
        App app = Axiom.create();

        // Global middleware: request logging
        app.use((ctx, next) -> {
            long start = System.nanoTime();
            next.run();
            long duration = (System.nanoTime() - start) / 1_000_000;
            LOG.info("{} {} ({}ms)", ctx.method(), ctx.path(), duration);
        });

        // Global middleware: CORS headers
        app.use((ctx, next) -> {
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
            ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            if ("OPTIONS".equals(ctx.method())) {
                ctx.status(204);
                return;
            }
            next.run();
        });

        // Health check (public)
        app.before(ctx -> {
            if ("/health".equals(ctx.path())) {
                ctx.json(Map.of("status", "ok", "timestamp", System.currentTimeMillis()));
            }
        });

        // ========== 5. Register Routes ==========
        // Public auth routes: /auth/*
        app.route("/auth", services.authRoutes.router());

        // Protected user routes: /users/* (with auth middleware)
        app.use((ctx, next) -> {
            if (ctx.path().startsWith("/users")) {
                services.authMiddleware.handle(ctx, next);
            } else {
                next.run();
            }
        });
        app.route("/users", services.userRoutes.router());

        // Custom error handler
        app.onError((ctx, e) -> {
            LOG.error("Request error: {}", e.getMessage(), e);
            ctx.status(500);
            ctx.json(Map.of(
                    "error", "Internal Server Error",
                    "message", e.getMessage()
            ));
        });

        // ========== 6. Lifecycle Hooks ==========
        app.onStart(() -> {
            LOG.info("Initializing database schema...");
            services.userRepository.initSchema();
        });

        app.onReady(() -> {
            LOG.info("Application ready! Try:");
            LOG.info("  curl http://localhost:{}/health", appConfig.server().port());
            LOG.info("  curl -X POST http://localhost:{}/auth/register -H 'Content-Type: application/json' \\",
                    appConfig.server().port());
            LOG.info("       -d '{{\"email\":\"test@example.com\",\"password\":\"password123\",\"name\":\"Test User\"}}'");
        });

        app.onShutdown(() -> {
            LOG.info("Shutting down application...");
            AxiomPersistence.stop();
        });

        // ========== 7. Start Server ==========
        app.listen(appConfig.server().host(), appConfig.server().port());
    }
}
