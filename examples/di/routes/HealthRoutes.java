package playground.di.routes;

import io.axiom.core.routing.Router;
import io.axiom.di.Routes;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * Health check routes.
 *
 * @Routes("/health") — auto-mounted at /health
 */
@Routes("/health")
public class HealthRoutes {

    @Inject
    public HealthRoutes() {}

    public Router router() {
        Router r = new Router();

        r.get("/", ctx -> {
            ctx.json(Map.of(
                "status", "healthy",
                "timestamp", System.currentTimeMillis()
            ));
        });

        r.get("/ready", ctx -> {
            ctx.json(Map.of(
                "ready", true,
                "checks", Map.of(
                    "database", "ok",
                    "cache", "ok"
                )
            ));
        });

        r.get("/live", ctx -> {
            ctx.text("OK");
        });

        return r;
    }
}
