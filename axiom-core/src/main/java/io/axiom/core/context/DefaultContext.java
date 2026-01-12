package io.axiom.core.context;

import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import io.axiom.core.error.*;
import io.axiom.core.json.*;
import io.axiom.core.middleware.*;

/**
 * Default implementation of {@link Context}.
 *
 * <p>
 * This implementation is designed for use with runtime adapters.
 * It wraps the raw request/response from the underlying HTTP server
 * and provides the unified Axiom API.
 *
 * <h2>Thread Safety</h2>
 * <p>
 * A single Context instance is NOT thread-safe and should only be
 * accessed from the request-handling thread. This is safe because
 * each request runs on its own virtual thread.
 *
 * @since 0.1.0
 */
public final class DefaultContext implements Context {

    private final Request request;
    private final Response response;
    private final JsonCodec jsonCodec;
    private final Map<String, Object> state;
    private final AtomicBoolean committed;

    private Object cachedBody;
    private boolean bodyParsed;

    /**
     * Creates a new context.
     *
     * @param request   the wrapped request
     * @param response  the wrapped response
     * @param jsonCodec the JSON codec for body parsing
     */
    public DefaultContext(final Request request, final Response response, final JsonCodec jsonCodec) {
        this.request = Objects.requireNonNull(request, "Request cannot be null");
        this.response = Objects.requireNonNull(response, "Response cannot be null");
        this.jsonCodec = Objects.requireNonNull(jsonCodec, "JsonCodec cannot be null");
        this.state = new HashMap<>();
        this.committed = new AtomicBoolean(false);
    }

    // ========== Request Methods ==========

    @Override
    public String method() {
        return this.request.method();
    }

    @Override
    public String path() {
        return this.request.path();
    }

    @Override
    public String param(final String name) {
        Objects.requireNonNull(name, "Parameter name cannot be null");
        return this.request.params().get(name);
    }

    @Override
    public String query(final String name) {
        Objects.requireNonNull(name, "Query parameter name cannot be null");
        return this.request.queryParams().get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T body(final Class<T> type) {
        Objects.requireNonNull(type, "Body type cannot be null");

        if (this.bodyParsed) {
            return type.cast(this.cachedBody);
        }

        try {
            final byte[] rawBody = this.request.body();
            if (rawBody == null || rawBody.length == 0) {
                this.cachedBody = null;
            } else if (type == String.class) {
                this.cachedBody = new String(rawBody, StandardCharsets.UTF_8);
            } else if (type == byte[].class) {
                this.cachedBody = rawBody;
            } else {
                this.cachedBody = this.jsonCodec.deserialize(rawBody, type);
            }
            this.bodyParsed = true;
            return (T) this.cachedBody;
        } catch (final JsonException e) {
            throw new BodyParseException("Failed to parse request body as " + type.getName(), type, e);
        }
    }

    @Override
    public Map<String, String> headers() {
        return this.request.headers();
    }

    // ========== Response Methods ==========

    @Override
    public void status(final int code) {
        this.ensureNotCommitted();
        this.response.status(code);
    }

    @Override
    public void header(final String name, final String value) {
        Objects.requireNonNull(name, "Header name cannot be null");
        Objects.requireNonNull(value, "Header value cannot be null");
        this.ensureNotCommitted();
        this.response.header(name, value);
    }

    @Override
    public void text(final String value) {
        Objects.requireNonNull(value, "Text value cannot be null");
        this.ensureNotCommitted();
        this.response.header("Content-Type", "text/plain; charset=UTF-8");
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        this.commit(bytes);
    }

    @Override
    public void json(final Object value) {
        this.ensureNotCommitted();
        this.response.header("Content-Type", "application/json");
        final byte[] bytes = this.jsonCodec.serializeToBytes(value);
        this.commit(bytes);
    }

    @Override
    public void send(final byte[] data) {
        Objects.requireNonNull(data, "Data cannot be null");
        this.ensureNotCommitted();
        this.commit(data);
    }

    // ========== State Methods ==========

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(final String key, final Class<T> type) {
        Objects.requireNonNull(key, "State key cannot be null");
        Objects.requireNonNull(type, "Type cannot be null");

        final Object value = this.state.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (!type.isInstance(value)) {
            return Optional.empty();
        }
        return Optional.of((T) value);
    }

    @Override
    public void set(final String key, final Object value) {
        Objects.requireNonNull(key, "State key cannot be null");
        this.state.put(key, value);
    }

    // ========== Middleware Support ==========

    private Next nextHandler;

    /**
     * Sets the next handler for middleware chain support.
     *
     * <p>
     * This is called by the middleware adapter to enable {@code ctx.next()} style.
     *
     * @param next the next handler in the chain
     */
    public void setNext(final Next next) {
        this.nextHandler = next;
    }

    @Override
    public void next() throws Exception {
        if (this.nextHandler == null) {
            throw new UnsupportedOperationException(
                    "next() is only available in middleware context. " +
                            "Use app.use(ctx -> { ctx.next(); }) style middleware.");
        }
        this.nextHandler.run();
    }

    // ========== Internal ==========

    /**
     * Sets the path parameters extracted from route matching.
     *
     * @param params the path parameters
     */
    public void setPathParams(final Map<String, String> params) {
        if (params != null) {
            this.request.setParams(params);
        }
    }

    private void ensureNotCommitted() {
        if (this.committed.get()) {
            throw new ResponseCommittedException();
        }
    }

    private void commit(final byte[] data) {
        if (this.committed.compareAndSet(false, true)) {
            this.response.send(data);
        }
    }

    /**
     * Request abstraction for runtime adapters.
     */
    public interface Request {
        String method();
        String path();
        Map<String, String> params();
        void setParams(Map<String, String> params);
        Map<String, String> queryParams();
        Map<String, String> headers();
        byte[] body();
    }

    /**
     * Response abstraction for runtime adapters.
     */
    public interface Response {
        void status(int code);
        void header(String name, String value);
        void send(byte[] data);
    }
}
