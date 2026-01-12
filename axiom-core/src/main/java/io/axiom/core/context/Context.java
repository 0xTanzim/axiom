package io.axiom.core.context;

import java.util.*;

import io.axiom.core.handler.*;

/**
 * Request/response context providing access to all request data
 * and response writing capabilities.
 *
 * <p>
 * Context is the primary DX surface for Axiom. It combines
 * request reading, response writing, and request-scoped state
 * in a single, intuitive interface.
 *
 * <h2>Design Principles</h2>
 * <ul>
 * <li>Request data is read-only and immutable</li>
 * <li>Response can only be written once</li>
 * <li>State storage is type-safe</li>
 * <li>No annotations or magic</li>
 * </ul>
 *
 * <h2>Request Data</h2>
 *
 * <pre>{@code
 * String method = c.method(); // "GET", "POST", etc.
 * String path = c.path(); // "/users/123"
 * String id = c.param("id"); // Path parameter
 * String page = c.query("page"); // Query parameter
 * User user = c.body(User.class); // Parsed request body
 * Map<String, String> headers = c.headers();
 * }</pre>
 *
 * <h2>Response Writing</h2>
 *
 * <pre>{@code
 * c.status(201); // Set status code
 * c.header("X-Custom", "value"); // Set response header
 * c.text("Hello"); // Send text response
 * c.json(user); // Send JSON response
 * c.send(bytes); // Send raw bytes
 * }</pre>
 *
 * <h2>State Storage</h2>
 *
 * <pre>{@code
 * // In middleware
 * c.set("user", authenticatedUser);
 *
 * // In handler
 * User user = c.get("user", User.class).orElseThrow();
 * }</pre>
 *
 * @see Handler
 * @since 0.1.0
 */
public interface Context {

    // ========== Request Methods ==========

    /**
     * Returns the HTTP method of the request.
     *
     * @return HTTP method in uppercase (GET, POST, PUT, DELETE, etc.)
     */
    String method();

    /**
     * Returns the request path without query string.
     *
     * @return request path, e.g., "/users/123"
     */
    String path();

    /**
     * Returns a path parameter value.
     *
     * <p>
     * Path parameters are defined in route patterns using colon prefix:
     *
     * <pre>{@code
     * router.get("/users/:id", c -> {
     *     String id = c.param("id");
     * });
     * }</pre>
     *
     * @param name the parameter name (without colon)
     * @return the parameter value, or null if not present
     */
    String param(String name);

    /**
     * Returns a query parameter value.
     *
     * <p>
     * For URL {@code /search?q=java&page=2}:
     *
     * <pre>{@code
     * String q = c.query("q"); // "java"
     * String page = c.query("page"); // "2"
     * }</pre>
     *
     * @param name the query parameter name
     * @return the parameter value, or null if not present
     */
    String query(String name);

    /**
     * Returns the request body parsed as the specified type.
     *
     * <p>
     * Body is parsed once and cached. Subsequent calls return
     * the cached value.
     *
     * <p>
     * For JSON content type, the body is deserialized using
     * the configured JSON codec.
     *
     * @param <T>  the target type
     * @param type the class to parse the body as
     * @return the parsed body
     * @throws io.axiom.core.error.BodyParseException if parsing fails
     */
    <T> T body(Class<T> type);

    /**
     * Returns all request headers.
     *
     * <p>
     * Header names are case-insensitive per HTTP specification.
     * The returned map is unmodifiable.
     *
     * @return unmodifiable map of header name to value
     */
    Map<String, String> headers();

    /**
     * Returns a specific request header value.
     *
     * @param name the header name (case-insensitive)
     * @return the header value, or null if not present
     */
    default String header(final String name) {
        return this.headers().get(name);
    }

    // ========== Response Methods ==========

    /**
     * Sets the HTTP response status code.
     *
     * <p>
     * Must be called before writing the response body.
     * If not called, defaults to 200 OK.
     *
     * @param code HTTP status code (e.g., 200, 201, 404, 500)
     * @throws io.axiom.core.error.ResponseCommittedException if response already
     *                                                        sent
     */
    void status(int code);

    /**
     * Sets a response header.
     *
     * <p>
     * Must be called before writing the response body.
     *
     * @param name  the header name
     * @param value the header value
     * @throws io.axiom.core.error.ResponseCommittedException if response already
     *                                                        sent
     */
    void header(String name, String value);

    /**
     * Sends a plain text response.
     *
     * <p>
     * Sets Content-Type to "text/plain; charset=UTF-8" and writes
     * the response body.
     *
     * @param value the text content
     * @throws io.axiom.core.error.ResponseCommittedException if response already
     *                                                        sent
     */
    void text(String value);

    /**
     * Sends a JSON response.
     *
     * <p>
     * Sets Content-Type to "application/json" and serializes
     * the object using the configured JSON codec.
     *
     * @param value the object to serialize as JSON
     * @throws io.axiom.core.error.ResponseCommittedException if response already
     *                                                        sent
     */
    void json(Object value);

    /**
     * Sends raw bytes as response body.
     *
     * <p>
     * Content-Type should be set manually before calling this method.
     *
     * @param data the raw byte content
     * @throws io.axiom.core.error.ResponseCommittedException if response already
     *                                                        sent
     */
    void send(byte[] data);

    // ========== State Methods ==========

    /**
     * Retrieves a value from request-scoped state.
     *
     * <p>
     * State is used to pass data between middleware and handlers:
     *
     * <pre>{@code
     * // In auth middleware
     * c.set("user", authenticatedUser);
     *
     * // In handler
     * User user = c.get("user", User.class).orElseThrow();
     * }</pre>
     *
     * @param <T>  the expected type
     * @param key  the state key
     * @param type the expected class type
     * @return Optional containing the value if present and type matches
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * Stores a value in request-scoped state.
     *
     * <p>
     * Values are available for the duration of the request
     * and can be retrieved by downstream middleware and handlers.
     *
     * @param key   the state key
     * @param value the value to store
     */
    void set(String key, Object value);

    /**
     * Retrieves a value from request-scoped state, or returns default.
     *
     * @param <T>          the expected type
     * @param key          the state key
     * @param type         the expected class type
     * @param defaultValue value to return if key not present
     * @return the stored value or defaultValue
     */
    default <T> T getOrDefault(final String key, final Class<T> type, final T defaultValue) {
        return this.get(key, type).orElse(defaultValue);
    }

    // ========== Middleware Support ==========

    /**
     * Continues execution to the next middleware or handler.
     *
     * <p>
     * This method is only available when the context is used within
     * middleware. It provides an alternative DX style:
     *
     * <pre>{@code
     * // Style 1: Explicit next parameter
     * app.use((ctx, next) -> {
     *     log(ctx.path());
     *     next.run();
     * });
     *
     * // Style 2: Context-embedded next (same behavior)
     * app.use(ctx -> {
     *     log(ctx.path());
     *     ctx.next();
     * });
     * }</pre>
     *
     * <p>
     * Both styles are first-class and produce identical behavior.
     * Use whichever fits your mental model.
     *
     * @throws Exception                     if the next handler throws
     * @throws UnsupportedOperationException if called outside middleware context
     */
    default void next() throws Exception {
        throw new UnsupportedOperationException(
                "next() is only available in middleware context. " +
                        "Use app.use(ctx -> { ctx.next(); }) style middleware.");
    }
}
