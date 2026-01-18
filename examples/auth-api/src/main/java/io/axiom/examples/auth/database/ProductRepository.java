package io.axiom.examples.auth.database;

import io.axiom.persistence.*;
import io.axiom.persistence.config.*;
import io.axiom.persistence.jdbc.*;
import io.axiom.persistence.tx.*;
import io.axiom.di.*;

import java.util.*;

/**
 * Database example demonstrating Axiom Persistence.
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // 1. Start persistence (once at app startup)
 * AxiomPersistence.start();
 *
 * // 2. Use transactions
 * Transaction.execute(() -> {
 *     Jdbc.update("INSERT INTO users (name) VALUES (?)", "John");
 * });
 *
 * // 3. Shutdown (at app shutdown)
 * AxiomPersistence.stop();
 * }</pre>
 *
 * <h2>Configuration</h2>
 * Create {@code src/main/resources/application.properties}:
 * <pre>
 * axiom.persistence.url=jdbc:h2:mem:testdb
 * axiom.persistence.username=sa
 * axiom.persistence.password=
 * axiom.persistence.pool.size=10
 * </pre>
 */
@Repository
public class ProductRepository {

    /**
     * Initialize database schema (call once at startup)
     */
    public void initSchema() {
        Transaction.execute(() -> {
            Jdbc.update("""
                CREATE TABLE IF NOT EXISTS products (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(255) NOT NULL,
                    description TEXT,
                    price DECIMAL(10,2) NOT NULL,
                    stock INT NOT NULL DEFAULT 0,
                    active BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        });
    }

    /**
     * Find all products
     */
    public List<Product> findAll() {
        return Transaction.execute(() ->
            Jdbc.query(
                "SELECT * FROM products WHERE active = true ORDER BY name",
                this::mapProduct
            )
        );
    }

    /**
     * Find product by ID
     */
    public Optional<Product> findById(long id) {
        return Transaction.execute(() ->
            Jdbc.queryOne(
                "SELECT * FROM products WHERE id = ?",
                this::mapProduct,
                id
            )
        );
    }

    /**
     * Save a new product
     */
    public Product save(Product product) {
        return Transaction.execute(() -> {
            long id = Jdbc.insertAndReturnKey(
                "INSERT INTO products (name, description, price, stock, active) VALUES (?, ?, ?, ?, ?)",
                product.name(),
                product.description(),
                product.price(),
                product.stock(),
                product.active()
            );
            return new Product(id, product.name(), product.description(),
                product.price(), product.stock(), product.active());
        });
    }

    /**
     * Update product stock (demonstrates transaction)
     */
    public boolean updateStock(long productId, int quantity) {
        return Transaction.execute(() -> {
            int updated = Jdbc.update(
                "UPDATE products SET stock = stock + ? WHERE id = ? AND active = true",
                quantity,
                productId
            );
            return updated > 0;
        });
    }

    /**
     * Delete product (soft delete)
     */
    public boolean delete(long id) {
        return Transaction.execute(() -> {
            int updated = Jdbc.update(
                "UPDATE products SET active = false WHERE id = ?",
                id
            );
            return updated > 0;
        });
    }

    /**
     * Batch insert example
     */
    public void saveAll(List<Product> products) {
        Transaction.execute(() -> {
            for (Product p : products) {
                Jdbc.update(
                    "INSERT INTO products (name, description, price, stock) VALUES (?, ?, ?, ?)",
                    p.name(), p.description(), p.price(), p.stock()
                );
            }
        });
    }

    /**
     * Transaction with rollback example
     */
    public void transferStock(long fromProductId, long toProductId, int quantity) {
        Transaction.execute(() -> {
            // Deduct from source
            int deducted = Jdbc.update(
                "UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?",
                quantity, fromProductId, quantity
            );

            if (deducted == 0) {
                throw new RuntimeException("Insufficient stock");
                // Transaction will rollback automatically
            }

            // Add to destination
            Jdbc.update(
                "UPDATE products SET stock = stock + ? WHERE id = ?",
                quantity, toProductId
            );
        });
    }

    // Map ResultSet row to Product
    private Product mapProduct(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Product(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getDouble("price"),
            rs.getInt("stock"),
            rs.getBoolean("active")
        );
    }

    // Product record
    public record Product(
        long id,
        String name,
        String description,
        double price,
        int stock,
        boolean active
    ) {
        public static Product create(String name, String description, double price, int stock) {
            return new Product(0, name, description, price, stock, true);
        }
    }
}
