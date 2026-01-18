package io.axiom.examples.auth.domain;

/**
 * Example using Java Records with Axiom (RECOMMENDED).
 *
 * <p>Records are the FIRST-CLASS choice for:
 * <ul>
 *   <li>DTOs (request/response bodies)</li>
 *   <li>Value objects</li>
 *   <li>Immutable data</li>
 * </ul>
 *
 * <p>Benefits:
 * <ul>
 *   <li>No dependency (built into Java 25)</li>
 *   <li>Immutable by default</li>
 *   <li>Compact syntax</li>
 *   <li>Pattern matching support</li>
 * </ul>
 */
public record ProductRecord(
    Long id,
    String name,
    String description,
    double price,
    int stock,
    boolean active
) {
    /**
     * Example method - records can have methods too
     */
    public boolean isInStock() {
        return stock > 0 && active;
    }

    /**
     * Factory method for creating new products
     */
    public static ProductRecord create(String name, String description, double price, int stock) {
        return new ProductRecord(
            System.nanoTime(),
            name,
            description,
            price,
            stock,
            true
        );
    }
}
