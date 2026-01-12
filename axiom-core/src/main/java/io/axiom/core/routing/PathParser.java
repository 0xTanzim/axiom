package io.axiom.core.routing;

import java.util.*;

/**
 * Utility class for parsing URL paths into segments.
 *
 * <p>
 * Path parsing is performed once at route registration time.
 * The resulting segment list is immutable and stored in the route trie.
 *
 * <h2>Supported Syntax</h2>
 * <ul>
 * <li>{@code /users} - Static segment</li>
 * <li>{@code /users/:id} - Parameter segment</li>
 * <li>{@code /files/*} - Wildcard segment (must be last)</li>
 * </ul>
 *
 * <h2>Normalization Rules</h2>
 * <ul>
 * <li>Leading slash is required</li>
 * <li>Trailing slash is removed</li>
 * <li>Double slashes are collapsed</li>
 * <li>Empty segments are removed</li>
 * </ul>
 *
 * @since 0.1.0
 */
public final class PathParser {

    private PathParser() {
        // Utility class
    }

    /**
     * Parses a path pattern into a list of segments.
     *
     * @param path the path pattern (e.g., "/users/:id")
     * @return immutable list of segments
     * @throws IllegalArgumentException if path is invalid
     */
    public static List<Segment> parse(String path) {
        Objects.requireNonNull(path, "Path cannot be null");

        String normalized = normalize(path);
        if (normalized.isEmpty()) {
            return List.of();
        }

        String[] parts = normalized.split("/");
        List<Segment> segments = new ArrayList<>(parts.length);
        boolean hasWildcard = false;

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            if (hasWildcard) {
                throw new IllegalArgumentException(
                        "Wildcard must be the last segment: " + path);
            }

            Segment segment = parseSegment(part, path);
            if (segment instanceof WildcardSegment) {
                hasWildcard = true;
            }
            segments.add(segment);
        }

        return Collections.unmodifiableList(segments);
    }

    /**
     * Normalizes a path for consistent processing.
     *
     * @param path the raw path
     * @return normalized path without leading/trailing slashes
     */
    public static String normalize(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }

        // Remove leading slash
        String result = path.startsWith("/") ? path.substring(1) : path;

        // Remove trailing slash
        if (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }

        // Collapse double slashes
        while (result.contains("//")) {
            result = result.replace("//", "/");
        }

        return result;
    }

    /**
     * Joins a base path with a sub-path.
     *
     * @param base the base path
     * @param path the path to append
     * @return combined normalized path
     */
    public static String join(String base, String path) {
        String normalizedBase = normalize(base);
        String normalizedPath = normalize(path);

        if (normalizedBase.isEmpty()) {
            return "/" + normalizedPath;
        }
        if (normalizedPath.isEmpty()) {
            return "/" + normalizedBase;
        }

        return "/" + normalizedBase + "/" + normalizedPath;
    }

    private static Segment parseSegment(String part, String originalPath) {
        if (part.equals("*")) {
            return WildcardSegment.INSTANCE;
        }

        if (part.startsWith(":")) {
            String name = part.substring(1);
            if (name.isEmpty()) {
                throw new IllegalArgumentException(
                        "Parameter name cannot be empty: " + originalPath);
            }
            return new ParamSegment(name);
        }

        return new StaticSegment(part);
    }
}
