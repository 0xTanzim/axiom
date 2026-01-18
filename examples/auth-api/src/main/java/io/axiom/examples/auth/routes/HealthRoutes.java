package io.axiom.examples.auth.routes;

import io.axiom.core.routing.Router;
import io.axiom.di.Routes;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * Health check routes.
 *
 * <p>Simple status endpoints for monitoring.
 */
@Routes("/health")
public class HealthRoutes {

    @Inject
    public HealthRoutes() {}

    public Router router() {
        var router = new Router();

        // GET /health - Basic health check
        router.get("/", ctx -> {
            ctx.json(Map.of(
                "status", "healthy",
                "timestamp", System.currentTimeMillis()
            ));
        });

        // GET /health/ready - Readiness probe
        router.get("/ready", ctx -> {
            ctx.json(Map.of(
                "ready", true,
                "checks", Map.of(
                    "database", "ok",
                    "cache", "ok"
                )
            ));
        });

        // GET /health/live - Liveness probe
        router.get("/live", ctx -> {
            ctx.text("OK");
        });

        return router;
    }
}
