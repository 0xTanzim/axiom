package io.axiom.server;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import com.sun.net.httpserver.*;

import io.axiom.core.context.*;
import io.axiom.core.handler.*;
import io.axiom.core.json.*;
import io.axiom.core.server.*;

/**
 * JDK HttpServer implementation for Axiom.
 *
 * <p>
 * Uses Java's built-in {@code com.sun.net.httpserver.HttpServer}
 * with virtual threads for high concurrency without external dependencies.
 *
 * <h2>Design</h2>
 * <ul>
 *   <li>All errors propagate to the application handler (no internal handling)</li>
 *   <li>Virtual threads by default for massive concurrency</li>
 *   <li>Respects ServerConfig for all tuning parameters</li>
 *   <li>Graceful shutdown with configurable drain timeout</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <p>
 * JdkServer does NOT handle errors internally. All exceptions from
 * the handler propagate through the composed handler chain, where
 * {@link io.axiom.core.app.DefaultApp} applies the configured error handler.
 *
 * @since 0.1.0
 */
final class JdkServer implements Server {

    private static final int DEFAULT_BACKLOG = 0;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private Handler handler;
    private HttpServer httpServer;
    private ExecutorService executor;
    private ServerConfig config;

    @Override
    public Server handler(final Handler handler) {
        if (this.started.get()) {
            throw new IllegalStateException("Cannot set handler after server has started");
        }
        this.handler = Objects.requireNonNull(handler, "Handler cannot be null");
        return this;
    }

    @Override
    public void start(final ServerConfig config) {
        Objects.requireNonNull(config, "ServerConfig cannot be null");

        if (this.handler == null) {
            throw new IllegalStateException("Handler must be set before starting server");
        }

        if (!this.started.compareAndSet(false, true)) {
            throw new IllegalStateException("Server already started");
        }

        this.config = config;

        try {
            this.executor = config.virtualThreads()
                    ? Executors.newVirtualThreadPerTaskExecutor()
                    : Executors.newCachedThreadPool();

            final InetSocketAddress address = new InetSocketAddress(config.host(), config.port());
            this.httpServer = HttpServer.create(address, JdkServer.DEFAULT_BACKLOG);
            this.httpServer.setExecutor(this.executor);

            this.httpServer.createContext("/", this::handleExchange);
            this.httpServer.start();

        } catch (final IOException e) {
            this.started.set(false);
            throw new RuntimeException("Failed to start server on " + config.host() + ":" + config.port(), e);
        }
    }

    @Override
    public void stop() {
        if (!this.stopped.compareAndSet(false, true)) {
            return;
        }

        if (this.httpServer != null) {
            final int drainSeconds = (int) this.config.drainTimeout().toSeconds();
            this.httpServer.stop(drainSeconds);
            this.httpServer = null;
        }

        if (this.executor != null) {
            this.executor.shutdown();
            try {
                final long timeout = this.config.shutdownTimeout().toMillis();
                if (!this.executor.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                    this.executor.shutdownNow();
                }
            } catch (final InterruptedException e) {
                this.executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            this.executor = null;
        }

        this.started.set(false);
    }

    @Override
    public int port() {
        if (this.httpServer == null) {
            return -1;
        }
        return this.httpServer.getAddress().getPort();
    }

    @Override
    public boolean isRunning() {
        return this.started.get() && !this.stopped.get() && this.httpServer != null;
    }

    private void handleExchange(final HttpExchange exchange) {
        try {
            final var request = new JdkRequest(exchange, this.config.maxRequestSize());
            final var response = new JdkResponse(exchange);
            final var jsonCodec = new JacksonCodec();
            final var context = new DefaultContext(request, response, jsonCodec);

            this.handler.handle(context);

            if (!response.isCommitted()) {
                response.status(204);
                response.send(new byte[0]);
            }
        } catch (final Exception e) {
            this.sendErrorResponse(exchange, e);
        } finally {
            exchange.close();
        }
    }

    private void sendErrorResponse(final HttpExchange exchange, final Exception error) {
        try {
            final String body = "{\"error\":\"Internal Server Error\",\"message\":\"" +
                    JdkServer.escapeJson(error.getMessage() != null ? error.getMessage() : "Unknown error") + "\"}";
            final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, bytes.length);

            try (final OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (final IOException ignored) {
            // Response already committed or connection closed
        }
    }

    private static String escapeJson(final String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * JDK HttpExchange request adapter.
     */
    private static final class JdkRequest implements DefaultContext.Request {
        private final HttpExchange exchange;
        private final int maxRequestSize;

        private Map<String, String> params = Collections.emptyMap();
        private Map<String, String> queryParams;
        private Map<String, String> headers;
        private byte[] body;
        private boolean bodyRead;

        JdkRequest(final HttpExchange exchange, final int maxRequestSize) {
            this.exchange = exchange;
            this.maxRequestSize = maxRequestSize;
        }

        @Override
        public String method() {
            return this.exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        }

        @Override
        public String path() {
            return this.exchange.getRequestURI().getPath();
        }

        @Override
        public Map<String, String> params() {
            return this.params;
        }

        @Override
        public void setParams(final Map<String, String> params) {
            this.params = params != null ? Collections.unmodifiableMap(new HashMap<>(params)) : Collections.emptyMap();
        }

        @Override
        public Map<String, String> queryParams() {
            if (this.queryParams == null) {
                this.queryParams = this.parseQueryParams();
            }
            return this.queryParams;
        }

        @Override
        public Map<String, String> headers() {
            if (this.headers == null) {
                this.headers = this.parseHeaders();
            }
            return this.headers;
        }

        @Override
        public byte[] body() {
            if (!this.bodyRead) {
                this.body = this.readBody();
                this.bodyRead = true;
            }
            return this.body;
        }

        private Map<String, String> parseQueryParams() {
            final String query = this.exchange.getRequestURI().getQuery();
            if (query == null || query.isEmpty()) {
                return Collections.emptyMap();
            }

            final Map<String, String> result = new HashMap<>();
            for (final String pair : query.split("&")) {
                final int idx = pair.indexOf('=');
                if (idx > 0) {
                    final String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                    final String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                    result.put(key, value);
                } else if (!pair.isEmpty()) {
                    result.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
                }
            }
            return Collections.unmodifiableMap(result);
        }

        private Map<String, String> parseHeaders() {
            final Map<String, String> result = new HashMap<>();
            for (final var entry : this.exchange.getRequestHeaders().entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    result.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue().getFirst());
                }
            }
            return Collections.unmodifiableMap(result);
        }

        private byte[] readBody() {
            try (final InputStream is = this.exchange.getRequestBody();
                 final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                final byte[] buffer = new byte[8192];
                int bytesRead;
                int totalRead = 0;

                while ((bytesRead = is.read(buffer)) != -1) {
                    totalRead += bytesRead;
                    if (totalRead > this.maxRequestSize) {
                        throw new RuntimeException("Request body exceeds maximum size of " + this.maxRequestSize + " bytes");
                    }
                    baos.write(buffer, 0, bytesRead);
                }

                return baos.toByteArray();
            } catch (final IOException e) {
                throw new RuntimeException("Failed to read request body", e);
            }
        }
    }

    /**
     * JDK HttpExchange response adapter.
     */
    private static final class JdkResponse implements DefaultContext.Response {
        private final HttpExchange exchange;
        private final AtomicBoolean committed = new AtomicBoolean(false);
        private int statusCode = 200;

        JdkResponse(final HttpExchange exchange) {
            this.exchange = exchange;
        }

        @Override
        public void status(final int code) {
            this.statusCode = code;
        }

        @Override
        public void header(final String name, final String value) {
            this.exchange.getResponseHeaders().set(name, value);
        }

        @Override
        public void send(final byte[] data) {
            if (!this.committed.compareAndSet(false, true)) {
                return;
            }

            try {
                final long length = data != null && data.length > 0 ? data.length : -1;
                this.exchange.sendResponseHeaders(this.statusCode, length);

                if (data != null && data.length > 0) {
                    try (final OutputStream os = this.exchange.getResponseBody()) {
                        os.write(data);
                    }
                }
            } catch (final IOException e) {
                throw new RuntimeException("Failed to send response", e);
            }
        }

        boolean isCommitted() {
            return this.committed.get();
        }
    }
}
