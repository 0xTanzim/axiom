package io.axiom.core.routing;

import java.util.Objects;

/**
 * A parameter segment that captures a single path component.
 *
 * <p>
 * Parameter segments are denoted with a colon prefix in route patterns
 * (e.g., ":id", ":name"). They match any single path segment and capture
 * its value.
 *
 * <h2>Examples</h2>
 * 
 * <pre>
 * "/users/:id"        → ParamSegment("id")
 * "/posts/:slug"      → ParamSegment("slug")
 * "/:resource/:id"    → ParamSegment("resource"), ParamSegment("id")
 * </pre>
 *
 * <h2>Captured Values</h2>
 * <p>
 * When a route matches, parameter values are available via
 * {@code Context.param(name)}:
 * 
 * <pre>{@code
 * router.get("/users/:id", c -> {
 *     String id = c.param("id"); // "123" for "/users/123"
 * });
 * }</pre>
 *
 * @param name the parameter name (without colon prefix)
 * @since 0.1.0
 */
public record ParamSegment(String name) implements Segment {

    /**
     * Creates a parameter segment.
     *
     * @param name the parameter name (without colon)
     * @throws IllegalArgumentException if name is null, empty, or invalid
     */
    public ParamSegment {
        Objects.requireNonNull(name, "Parameter name cannot be null");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Parameter name cannot be empty");
        }
        if (!isValidIdentifier(name)) {
            throw new IllegalArgumentException(
                    "Parameter name must be a valid identifier: " + name);
        }
    }

    /**
     * Returns the parameter name prefixed with colon.
     *
     * @return the segment as it appears in a route pattern
     */
    @Override
    public String value() {
        return ":" + name;
    }

    private static boolean isValidIdentifier(String name) {
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
