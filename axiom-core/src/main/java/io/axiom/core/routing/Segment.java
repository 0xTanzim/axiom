package io.axiom.core.routing;

/**
 * Represents a segment of a URL path.
 *
 * <p>
 * Paths are decomposed into segments at registration time and stored
 * in the route trie. This sealed hierarchy enables exhaustive pattern
 * matching for segment types.
 *
 * <h2>Segment Types</h2>
 * <ul>
 * <li>{@link StaticSegment} - Literal path component (e.g., "users")</li>
 * <li>{@link ParamSegment} - Named path parameter (e.g., ":id")</li>
 * <li>{@link WildcardSegment} - Catch-all remainder (e.g., "*")</li>
 * </ul>
 *
 * <h2>Path Examples</h2>
 * 
 * <pre>
 * "/users"           → [StaticSegment("users")]
 * "/users/:id"       → [StaticSegment("users"), ParamSegment("id")]
 * "/files/*"         → [StaticSegment("files"), WildcardSegment]
 * "/api/v1/:resource/:id" → [Static("api"), Static("v1"), Param("resource"), Param("id")]
 * </pre>
 *
 * <h2>Matching Precedence</h2>
 * <p>
 * At each level of the route trie, segments are matched in order:
 * <ol>
 * <li>Static segments (exact match)</li>
 * <li>Parameter segments (capture any single segment)</li>
 * <li>Wildcard segment (capture remaining path)</li>
 * </ol>
 *
 * @see StaticSegment
 * @see ParamSegment
 * @see WildcardSegment
 * @since 0.1.0
 */
public sealed interface Segment
        permits StaticSegment, ParamSegment, WildcardSegment {

    /**
     * Returns the original string representation of this segment.
     *
     * @return segment as string (e.g., "users", ":id", "*")
     */
    String value();
}
