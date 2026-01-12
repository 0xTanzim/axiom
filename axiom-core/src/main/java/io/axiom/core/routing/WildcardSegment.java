package io.axiom.core.routing;

/**
 * A wildcard segment that captures the remainder of the path.
 *
 * <p>
 * Wildcard segments are denoted with an asterisk (*) in route patterns.
 * They match zero or more remaining path segments and have the lowest
 * matching precedence.
 *
 * <h2>Examples</h2>
 * 
 * <pre>
 * "/files/*"      → Matches "/files/", "/files/a", "/files/a/b/c"
 * "/static/*"     → Matches "/static/css/style.css"
 * </pre>
 *
 * <h2>Constraints</h2>
 * <ul>
 * <li>Wildcard must be the last segment in a route</li>
 * <li>Only one wildcard per route</li>
 * <li>Cannot be combined with parameters after it</li>
 * </ul>
 *
 * <h2>Captured Values</h2>
 * <p>
 * The captured path remainder is available via {@code Context.param("*")}:
 * 
 * <pre>{@code
 * router.get("/files/*", c -> {
 *     String path = c.param("*"); // "a/b/c" for "/files/a/b/c"
 * });
 * }</pre>
 *
 * @since 0.1.0
 */
public record WildcardSegment() implements Segment {

    /**
     * The parameter name used to retrieve wildcard captures.
     */
    public static final String PARAM_NAME = "*";

    /**
     * Singleton instance since wildcard segments are stateless.
     */
    public static final WildcardSegment INSTANCE = new WildcardSegment();

    /**
     * Returns the wildcard character.
     *
     * @return "*"
     */
    @Override
    public String value() {
        return PARAM_NAME;
    }
}
