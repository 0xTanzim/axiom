package io.axiom.examples.auth.domain;

import lombok.*;

/**
 * Example using Lombok with Axiom.
 *
 * <p>Lombok is SUPPORTED but not required. Use it if you prefer.
 * Records are the FIRST-CLASS choice for immutable data.
 *
 * <h2>When to use Lombok vs Records</h2>
 * <ul>
 *   <li>Records: Immutable data, DTOs, value objects (recommended)</li>
 *   <li>Lombok: Mutable entities, builders, complex objects</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductLombok {

    private Long id;
    private String name;
    private String description;
    private double price;
    private int stock;
    private boolean active;

    /**
     * Example method - Lombok generates getters/setters/toString/equals/hashCode
     */
    public boolean isInStock() {
        return stock > 0 && active;
    }
}
