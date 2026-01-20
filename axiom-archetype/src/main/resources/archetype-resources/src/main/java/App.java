package ${package};

import io.axiom.core.app.Axiom;
import io.axiom.core.routing.Router;
import java.util.Map;

/**
 * Axiom Application - Your journey starts here!
 *
 * Run with:
 *   mvn compile exec:java
 *
 * Or build and run:
 *   mvn package
 *   java -jar target/${artifactId}-${version}.jar
 */
public class App {

    public static void main(String[] args) {
        Router router = new Router();

        // Hello World
        router.get("/", ctx -> ctx.text("Hello, Axiom!"));

        // JSON endpoint
        router.get("/api/status", ctx ->
            ctx.json(Map.of(
                "status", "running",
                "framework", "Axiom",
                "version", "0.1.1"
            ))
        );

        // Path parameters
        router.get("/users/:id", ctx -> {
            String id = ctx.param("id");
            ctx.json(Map.of("id", id, "name", "User " + id));
        });

        // Start server
        Axiom.start(router, 8080);
        System.out.println("🚀 Server running at http://localhost:8080");
    }
}
