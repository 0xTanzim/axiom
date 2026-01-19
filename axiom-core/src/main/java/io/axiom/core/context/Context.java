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
     * Returns a path parameter value or throws if missing.
     *
     * <p>
     * Use when the parameter is required and should never be null:
     *
     * <pre>{@code
     * router.get("/users/:id", c -> {
     *     String id = c.paramOrThrow("id"); // throws if missing
     * });
     * }</pre>
     *
     * @param name the parameter name (without colon)
     * @return the parameter value (never null)
     * @throws IllegalArgumentException if parameter is not present
     */
    default String paramOrThrow(final String name) {
        final String value = this.param(name);
        if (value == null) {
            throw new IllegalArgumentException("Required path parameter missing: " + name);
        }
        return value;
    }

    /**
     * Returns all path parameters as a map.
     *
     * <p>
     * The returned map is unmodifiable.
     *
     * @return unmodifiable map of parameter names to values
     */
    Map<String, String> params();

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
     * Returns a query parameter with a default value.
     *
     * @param name         the query parameter name
     * @param defaultValue value to return if parameter is missing
     * @return the parameter value or defaultValue
     */
    default String query(final String name, final String defaultValue) {
        final String value = this.query(name);
        return value != null ? value : defaultValue;
    }

    /**
     * Returns a query parameter as an integer.
     *
     * @param name the query parameter name
     * @return the parsed integer value
     * @throws NumberFormatException    if value cannot be parsed
     * @throws IllegalArgumentException if parameter is missing
     */
    default int queryInt(final String name) {
        final String value = this.query(name);
        if (value == null) {
            throw new IllegalArgumentException("Required query parameter missing: " + name);
        }
        return Integer.parseInt(value);
    }

    /**
     * Returns a query parameter as an integer with a default.
     *
     * @param name         the query parameter name
     * @param defaultValue value to return if parameter is missing or invalid
     * @return the parsed integer value or defaultValue
     */
    default int queryInt(final String name, final int defaultValue) {
        final String value = this.query(name);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Returns a query parameter as a long.
     *
     * @param name the query parameter name
     * @return the parsed long value
     * @throws NumberFormatException    if value cannot be parsed
     * @throws IllegalArgumentException if parameter is missing
     */
    default long queryLong(final String name) {
        final String value = this.query(name);
        if (value == null) {
            throw new IllegalArgumentException("Required query parameter missing: " + name);
        }
        return Long.parseLong(value);
    }

    /**
     * Returns a query parameter as a long with a default.
     *
     * @param name         the query parameter name
     * @param defaultValue value to return if parameter is missing or invalid
     * @return the parsed long value or defaultValue
     */
    default long queryLong(final String name, final long defaultValue) {
        final String value = this.query(name);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (final NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Returns a query parameter as a boolean.
     *
     * <p>
     * Returns true for: "true", "1", "yes", "on" (case-insensitive).
     * Returns false for all other values or if missing.
     *
     * @param name the query parameter name
     * @return the parsed boolean value
     */
    default boolean queryBoolean(final String name) {
        final String value = this.query(name);
        if (value == null) {
            return false;
        }
        return "true".equalsIgnoreCase(value)
            || "1".equals(value)
            || "yes".equalsIgnoreCase(value)
            || "on".equalsIgnoreCase(value);
    }

    /**
     * Returns all query parameters as a map.
     *
     * <p>
     * The returned map is unmodifiable.
     *
     * @return unmodifiable map of query parameter names to values
     */
    Map<String, String> queries();

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
     * Returns the request body as a dynamic Map (schemaless).
     *
     * <p>This is the Koa/Hono/Express style - no DTOs needed:
     *
     * <pre>{@code
     * router.post("/login", ctx -> {
     *     var body = ctx.bodyAsMap();
     *     String username = (String) body.get("username");
     *     String password = (String) body.get("password");
     *     // ... use directly
     * });
     * }</pre>
     *
     * <p>For type-safe apps, use {@link #body(Class)} with records instead.
     *
     * @return parsed JSON body as a Map
     * @throws io.axiom.core.error.BodyParseException if parsing fails
     */
    @SuppressWarnings("unchecked")
    default Map<String, Object> bodyAsMap() {
        return this.body((Class<Map<String, Object>>) (Class<?>) Map.class);
    }

    /**
     * Returns the raw request body as a String.
     *
     * <p>Useful for custom parsing or non-JSON content types.
     *
     * @return raw body string
     */
    String bodyRaw();

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

    /**
     * Returns the Content-Type header of the request.
     *
     * @return the content type, or null if not present
     */
    default String contentType() {
        return this.header("Content-Type");
    }

    /**
     * Returns the Accept header of the request.
     *
     * @return the accept header, or null if not present
     */
    default String accept() {
        return this.header("Accept");
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
     * This is the setter - for reading request headers use {@link #header(String)}.
     *
     * @param name  the header name
     * @param value the header value
     * @throws io.axiom.core.error.ResponseCommittedException if response already
     *                                                        sent
     */
    void setHeader(String name, String value);

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
     * @deprecated Use {@link #setHeader(String, String)} for clarity. This method
     *             will be removed in a future version.
     */
    @Deprecated(forRemoval = true, since = "0.2.0")
    default void header(final String name, final String value) {
        this.setHeader(name, value);
    }

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

    /**
     * Sends an HTML response.
     *
     * <p>
     * Sets Content-Type to "text/html; charset=UTF-8" and writes
     * the response body.
     *
     * @param html the HTML content
     * @throws io.axiom.core.error.ResponseCommittedException if response already
     *                                                        sent
     */
    default void html(final String html) {
        this.setHeader("Content-Type", "text/html; charset=UTF-8");
        this.text(html);
    }

    /**
     * Sends a redirect response (302 Found).
     *
     * @param url the URL to redirect to
     * @throws io.axiom.core.error.ResponseCommittedException if response already
     *                                                        sent
     */
    default void redirect(final String url) {
        this.redirect(url, 302);
    }

    /**
     * Sends a redirect response with a custom status code.
     *
     * @param url    the URL to redirect to
     * @param status the HTTP status code (301, 302, 303, 307, 308)
     * @throws io.axiom.core.error.ResponseCommittedException if response already
     *                                                        sent
     */
    default void redirect(final String url, final int status) {
        this.status(status);
        this.setHeader("Location", url);
        this.text("");
    }

    /**
     * Sends a 404 Not Found response.
     *
     * @throws io.axiom.core.error.ResponseCommittedException if response already
     *                                                        sent
     */
    default void notFound() {
        this.status(404);
        this.json(java.util.Map.of("error", "Not Found"));
    }

    /**
     * Sends a 400 Bad Request response with a message.
     *
     * @param message the error message
     * @throws io.axiom.core.error.ResponseCommittedException if response already
     *                                                        sent
     */
    default void badRequest(final String message) {
        this.status(400);
        this.json(java.util.Map.of("error", "Bad Request", "message", message));
    }

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
