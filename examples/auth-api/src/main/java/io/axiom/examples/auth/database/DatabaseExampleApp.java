package io.axiom.examples.auth.database;

import io.axiom.core.app.*;
import io.axiom.core.routing.*;
import io.axiom.persistence.*;
import io.axiom.persistence.config.*;

import java.util.*;

/**
 * Database example application.
 *
 * <h2>Prerequisites</h2>
 * Add to pom.xml:
 * <pre>{@code
 * <dependency>
 *     <groupId>io.axiom</groupId>
 *     <artifactId>axiom-persistence</artifactId>
 *     <version>${axiom.version}</version>
 * </dependency>
 * <dependency>
 *     <groupId>com.h2database</groupId>
 *     <artifactId>h2</artifactId>
 *     <version>2.2.224</version>
 * </dependency>
 * }</pre>
 *
 * <h2>Run</h2>
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.axiom.examples.auth.database.DatabaseExampleApp
 * </pre>
 *
 * <h2>Test</h2>
 * <pre>
 * # List products
 * curl http://localhost:8080/products
 *
 * # Create product
 * curl -X POST http://localhost:8080/products \
 *   -H "Content-Type: application/json" \
 *   -d '{"name":"Widget","description":"A useful widget","price":29.99,"stock":100}'
 *
 * # Get product
 * curl http://localhost:8080/products/1
 * </pre>
 */
public class DatabaseExampleApp {

    public static void main(String[] args) {
        // 1. Start persistence with H2 in-memory database
        AxiomPersistence.start(PersistenceConfig.builder()
            .url("jdbc:h2:mem:example;DB_CLOSE_DELAY=-1")
            .username("sa")
            .password("")
            .maximumPoolSize(5)
            .build());

        // 2. Initialize schema
        ProductRepository repo = new ProductRepository();
        repo.initSchema();

        // 3. Seed some data
        repo.saveAll(List.of(
            ProductRepository.Product.create("Laptop", "High-performance laptop", 999.99, 10),
            ProductRepository.Product.create("Mouse", "Wireless mouse", 29.99, 50),
            ProductRepository.Product.create("Keyboard", "Mechanical keyboard", 149.99, 25)
        ));

        // 4. Create HTTP API
        App app = Axiom.create();

        // Logging middleware
        app.use((ctx, next) -> {
            System.out.println(ctx.method() + " " + ctx.path());
            next.run();
        });

        // Product routes
        Router products = new Router();

        products.get("/", ctx -> {
            var all = repo.findAll();
            ctx.json(Map.of("products", all, "count", all.size()));
        });

        products.get("/:id", ctx -> {
            long id = Long.parseLong(ctx.param("id"));
            repo.findById(id).ifPresentOrElse(
                p -> ctx.json(p),
                () -> {
                    ctx.status(404);
                    ctx.json(Map.of("error", "Product not found"));
                }
            );
        });

        products.post("/", ctx -> {
            var body = ctx.bodyAsMap();
            var product = ProductRepository.Product.create(
                (String) body.get("name"),
                (String) body.get("description"),
                ((Number) body.get("price")).doubleValue(),
                ((Number) body.get("stock")).intValue()
            );
            var saved = repo.save(product);
            ctx.status(201);
            ctx.json(saved);
        });

        products.put("/:id/stock", ctx -> {
            long id = Long.parseLong(ctx.param("id"));
            var body = ctx.bodyAsMap();
            int quantity = ((Number) body.get("quantity")).intValue();

            if (repo.updateStock(id, quantity)) {
                ctx.json(Map.of("success", true));
            } else {
                ctx.status(404);
                ctx.json(Map.of("error", "Product not found or inactive"));
            }
        });

        products.delete("/:id", ctx -> {
            long id = Long.parseLong(ctx.param("id"));
            if (repo.delete(id)) {
                ctx.status(204);
                ctx.text("");
            } else {
                ctx.status(404);
                ctx.json(Map.of("error", "Product not found"));
            }
        });

        app.route("/products", products);

        // Health check - use Router since App doesn't have direct get() method
        Router health = new Router();
        health.get("/health", ctx -> ctx.json(Map.of(
            "status", "healthy",
            "database", AxiomPersistence.isStarted() ? "connected" : "disconnected"
        )));
        app.route(health);

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            AxiomPersistence.stop();
        }));

        // Start server
        app.listen(8080);
    }
}
