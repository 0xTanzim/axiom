package io.axiom.benchmark;

import io.axiom.core.app.Axiom;
import io.axiom.core.app.App;
import io.axiom.core.routing.Router;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Axiom benchmark application for wrk testing.
 */
public class Main {

    public static void main(String[] args) {
        Router router = new Router();

        // Test 1: Hello World (minimal overhead)
        router.get("/", ctx -> {
            ctx.json(Map.of("message", "Hello, World!"));
        });

        // Test 2: Path Parameters
        router.get("/users/:id", ctx -> {
            String id = ctx.param("id");
            ctx.json(Map.of(
                    "id", id,
                    "name", "User " + id
            ));
        });

        // Test 3: JSON Request/Response
        router.post("/users", ctx -> {
            var user = ctx.body(CreateUserRequest.class);
            ctx.json(Map.of(
                    "id", UUID.randomUUID().toString(),
                    "name", user.name(),
                    "email", user.email()
            ));
        });

        // Test 4: Query Parameters
        router.get("/search", ctx -> {
            String query = ctx.query("q");
            int limit = Integer.parseInt(ctx.query("limit", "10"));
            ctx.json(Map.of(
                    "query", query,
                    "limit", limit,
                    "results", List.of()
            ));
        });

        App app = Axiom.create();
        app.route(router);
        app.listen(8080);
    }

    record CreateUserRequest(String name, String email) {
    }
}
