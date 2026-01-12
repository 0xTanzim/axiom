package io.axiom.core.routing.internal;

import java.util.*;

import io.axiom.core.handler.Handler;
import io.axiom.core.routing.*;

/**
 * Trie-based route matching engine.
 *
 * <p>
 * This is an internal implementation class that provides O(depth) path matching
 * with deterministic precedence rules.
 *
 * <h2>Matching Precedence</h2>
 * <p>
 * At each trie level, segments are matched in order:
 * <ol>
 * <li>Static segments (exact string match)</li>
 * <li>Parameter segments (captures single segment)</li>
 * <li>Wildcard segment (captures remainder)</li>
 * </ol>
 *
 * <h2>Performance Characteristics</h2>
 * <ul>
 * <li>O(depth) matching where depth is path segment count</li>
 * <li>No regex evaluation</li>
 * <li>No backtracking</li>
 * <li>Single allocation for params map</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * The trie is mutable during route registration (startup) but
 * should be treated as read-only during request handling.
 * Route registration is NOT thread-safe. Request matching IS thread-safe
 * once registration is complete.
 *
 * @since 0.1.0
 */
public final class RouteTrie {

    private final Map<String, TrieNode> methodRoots;

    /**
     * Creates an empty route trie.
     */
    public RouteTrie() {
        this.methodRoots = new HashMap<>();
    }

    /**
     * Inserts a route into the trie.
     *
     * @param route the route to insert
     * @throws RouteConflictException if route conflicts with existing route
     */
    public void insert(Route route) {
        TrieNode root = methodRoots.computeIfAbsent(
                route.method(),
                _ -> new TrieNode());

        TrieNode current = root;
        List<Segment> segments = route.segments();

        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            current = insertSegment(current, segment, route, i == segments.size() - 1);
        }

        // Handle root path "/"
        if (segments.isEmpty()) {
            if (current.handler != null) {
                throw new RouteConflictException(route, "Duplicate route: " + route);
            }
            current.handler = route.handler();
            current.route = route;
        }
    }

    private TrieNode insertSegment(TrieNode node, Segment segment, Route route, boolean isLast) {
        TrieNode next;

        switch (segment) {
            case StaticSegment s -> {
                next = node.staticChildren.computeIfAbsent(s.value(), _ -> new TrieNode());
            }
            case ParamSegment p -> {
                if (node.paramChild == null) {
                    node.paramChild = new TrieNode();
                    node.paramName = p.name();
                } else if (!node.paramName.equals(p.name())) {
                    throw new RouteConflictException(route,
                            "Parameter name conflict: :" + node.paramName + " vs :" + p.name());
                }
                next = node.paramChild;
            }
            case WildcardSegment _ -> {
                if (node.wildcardChild == null) {
                    node.wildcardChild = new TrieNode();
                }
                next = node.wildcardChild;
            }
        }

        if (isLast) {
            if (next.handler != null) {
                throw new RouteConflictException(route, "Duplicate route: " + route);
            }
            next.handler = route.handler();
            next.route = route;
        }

        return next;
    }

    /**
     * Matches a request path against registered routes.
     *
     * @param method HTTP method
     * @param path   request path
     * @return RouteMatch if found, null otherwise
     */
    public RouteMatch match(String method, String path) {
        TrieNode root = methodRoots.get(method.toUpperCase());
        if (root == null) {
            return null;
        }

        String[] segments = splitPath(path);
        Map<String, String> params = new LinkedHashMap<>();

        TrieNode matched = matchSegments(root, segments, 0, params);
        if (matched == null || matched.handler == null) {
            return null;
        }

        return new RouteMatch(matched.route, params);
    }

    private TrieNode matchSegments(TrieNode node, String[] segments, int index,
            Map<String, String> params) {
        // Base case: consumed all segments
        if (index >= segments.length) {
            // If current node has a handler, return it
            if (node.handler != null) {
                return node;
            }
            // If no handler but there's a wildcard child, it captures empty remainder
            if (node.wildcardChild != null && node.wildcardChild.handler != null) {
                params.put(WildcardSegment.PARAM_NAME, "");
                return node.wildcardChild;
            }
            return node;
        }

        String segment = segments[index];

        // Priority 1: Static match
        TrieNode staticMatch = node.staticChildren.get(segment);
        if (staticMatch != null) {
            TrieNode result = matchSegments(staticMatch, segments, index + 1, params);
            if (result != null && result.handler != null) {
                return result;
            }
        }

        // Priority 2: Parameter match
        if (node.paramChild != null) {
            params.put(node.paramName, segment);
            TrieNode result = matchSegments(node.paramChild, segments, index + 1, params);
            if (result != null && result.handler != null) {
                return result;
            }
            params.remove(node.paramName);
        }

        // Priority 3: Wildcard match
        if (node.wildcardChild != null) {
            String remainder = joinRemaining(segments, index);
            params.put(WildcardSegment.PARAM_NAME, remainder);
            return node.wildcardChild;
        }

        return null;
    }

    /**
     * Returns all registered routes.
     *
     * @return list of all routes
     */
    public List<Route> routes() {
        List<Route> routes = new ArrayList<>();
        for (TrieNode root : methodRoots.values()) {
            collectRoutes(root, routes);
        }
        return routes;
    }

    /**
     * Returns all methods registered for a given path.
     *
     * <p>
     * Useful for generating "Allow" headers in 405 responses.
     *
     * @param path the request path
     * @return list of HTTP methods
     */
    public List<String> allowedMethods(String path) {
        List<String> methods = new ArrayList<>();
        String[] segments = splitPath(path);

        for (Map.Entry<String, TrieNode> entry : methodRoots.entrySet()) {
            Map<String, String> params = new HashMap<>();
            TrieNode matched = matchSegments(entry.getValue(), segments, 0, params);
            if (matched != null && matched.handler != null) {
                methods.add(entry.getKey());
            }
        }

        return methods;
    }

    /**
     * Checks if any route is registered for a path (regardless of method).
     *
     * @param path the request path
     * @return true if at least one method matches this path
     */
    public boolean hasPath(String path) {
        return !allowedMethods(path).isEmpty();
    }

    private void collectRoutes(TrieNode node, List<Route> routes) {
        if (node.route != null) {
            routes.add(node.route);
        }
        for (TrieNode child : node.staticChildren.values()) {
            collectRoutes(child, routes);
        }
        if (node.paramChild != null) {
            collectRoutes(node.paramChild, routes);
        }
        if (node.wildcardChild != null) {
            collectRoutes(node.wildcardChild, routes);
        }
    }

    private static String[] splitPath(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return new String[0];
        }

        String normalized = path.startsWith("/") ? path.substring(1) : path;
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.isEmpty()) {
            return new String[0];
        }

        return normalized.split("/");
    }

    private static String joinRemaining(String[] segments, int fromIndex) {
        if (fromIndex >= segments.length) {
            return "";
        }
        return String.join("/", java.util.Arrays.copyOfRange(segments, fromIndex, segments.length));
    }

    /**
     * Internal trie node.
     */
    private static final class TrieNode {
        final Map<String, TrieNode> staticChildren = new HashMap<>();
        TrieNode paramChild;
        String paramName;
        TrieNode wildcardChild;
        Handler handler;
        Route route;
    }
}
