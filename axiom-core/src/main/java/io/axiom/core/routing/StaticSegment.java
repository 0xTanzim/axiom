package io.axiom.core.routing;

import java.util.Objects;

/**
 * A static path segment representing a literal string match.
 *
 * <p>
 * Static segments have highest precedence during route matching.
 * They must match exactly (case-sensitive).
 *
 * <h2>Examples</h2>
 * 
 * <pre>
 * "/users"     → StaticSegment("users")
 * "/api/v1"    → StaticSegment("api"), StaticSegment("v1")
 * "/health"    → StaticSegment("health")
 * </pre>
 *
 * @param value the literal segment value (never null or empty)
 * @since 0.1.0
 */
public record StaticSegment(String value) implements Segment {

    /**
     * Creates a static segment.
     *
     * @param value the literal segment value
     * @throws IllegalArgumentException if value is null or empty
     */
    public StaticSegment {
        Objects.requireNonNull(value, "Static segment value cannot be null");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Static segment value cannot be empty");
        }
        if (value.startsWith(":") || value.equals("*")) {
            throw new IllegalArgumentException(
                    "Static segment cannot start with ':' or be '*': " + value);
        }
    }
}
