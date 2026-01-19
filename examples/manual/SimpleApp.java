package playground.manual;

import io.axiom.core.app.*;
import io.axiom.core.routing.*;

import java.util.*;

/**
 * Simplest possible Axiom application.
 *
 * Demonstrates:
 * - Router-only startup (no App needed)
 * - Basic route handlers
 * - JSON responses
 * - Query parameters
 *
 * Run:
 *   cd axiom-core && mvn compile
 *   cd ../playground/manual
 *   javac -cp ../../axiom-core/target/classes:../../axiom-core/target/dependency/* SimpleApp.java
 *   java -cp .:../../axiom-core/target/classes:../../axiom-core/target/dependency/* SimpleApp
 *
 * Test:
 *   curl http://localhost:8080/health
 *   curl http://localhost:8080/hello?name=World
 *   curl http://localhost:8080/echo -X POST -d '{"message":"hi"}'
 */
public class SimpleApp {

    public static void main(String[] args) {
        Router router = new Router();

        // Health check
        router.get("/health", ctx -> {
            ctx.json(Map.of(
                "status", "healthy",
                "timestamp", System.currentTimeMillis()
            ));
        });

        // Hello with query param
        router.get("/hello", ctx -> {
            String name = ctx.query("name", "Axiom");
            ctx.json(Map.of("message", "Hello, " + name + "!"));
        });

        // Echo POST body
        router.post("/echo", ctx -> {
            Map<String, Object> body = ctx.bodyAsMap();
            ctx.json(Map.of(
                "received", body,
                "timestamp", System.currentTimeMillis()
            ));
        });

        // Path parameters
        router.get("/users/:id", ctx -> {
            String id = ctx.paramOrThrow("id");
            ctx.json(Map.of(
                "userId", id,
                "message", "User details for ID: " + id
            ));
        });

        // Wildcard path
        router.get("/files/*", ctx -> {
            String path = ctx.param("*");
            ctx.json(Map.of(
                "path", path,
                "message", "File at: " + path
            ));
        });

        // Start server - ONE LINE!
        System.out.println("Starting server at http://localhost:8080");
        Axiom.start(router, 8080);
    }
}
