package io.axiom.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.axiom.core.context.Context;
import io.axiom.core.context.DefaultContext;
import io.axiom.core.handler.Handler;
import io.axiom.core.json.JacksonCodec;
import io.axiom.core.json.JsonCodec;
import io.axiom.core.server.Server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JDK HttpServer implementation with virtual thread support.
 *
 * <p>
 * This server uses Java's built-in {@code com.sun.net.httpserver.HttpServer}
 * with a virtual thread executor for handling requests.
 *
 * <h2>Threading Model</h2>
 * <p>
 * Each request runs on its own virtual thread via
 * {@code Executors.newVirtualThreadPerTaskExecutor()}. Virtual threads
 * are lightweight (few KB stack), allowing millions of concurrent requests
 * without manual thread pool sizing.
 *
 * <h2>Java 25 Improvements</h2>
 * <p>
 * JEP 491 (Java 25) eliminates virtual thread pinning when entering
 * synchronized blocks, making virtual threads fully compatible with
 * legacy libraries that use synchronized.
 *
 * <h2>Lifecycle</h2>
 * <pre>
 * JdkServer server = new JdkServer();
 * server.handler(appHandler);
 * server.start("0.0.0.0", 8080);
 * // ... running ...
 * server.stop();
 * </pre>
 *
 * @since 0.1.0
 */
public final class JdkServer implements Server {

    private static final int DEFAULT_BACKLOG = 0;
    private static final int STOP_DELAY_SECONDS = 1;

    private final JsonCodec jsonCodec;
    private final AtomicBoolean running;
    private final AtomicInteger boundPort;

    private Handler handler;
    private HttpServer httpServer;
    private ExecutorService executor;

    public JdkServer() {
        this.jsonCodec = new JacksonCodec();
        this.running = new AtomicBoolean(false);
        this.boundPort = new AtomicInteger(-1);
    }

    @Override
    public Server handler(final Handler handler) {
        Objects.requireNonNull(handler, "Handler cannot be null");
        if (this.running.get()) {
            throw new IllegalStateException("Cannot set handler after server is started");
        }
        this.handler = handler;
        return this;
    }

    @Override
    public void start(final String host, final int port) {
        Objects.requireNonNull(host, "Host cannot be null");

        if (this.handler == null) {
            throw new IllegalStateException("Handler must be set before starting server");
        }

        if (!this.running.compareAndSet(false, true)) {
            throw new IllegalStateException("Server is already running");
        }

        try {
            this.executor = Executors.newVirtualThreadPerTaskExecutor();
            this.httpServer = HttpServer.create(new InetSocketAddress(host, port), DEFAULT_BACKLOG);
            this.httpServer.setExecutor(this.executor);
            this.httpServer.createContext("/", this::handleRequest);
            this.httpServer.start();
            this.boundPort.set(this.httpServer.getAddress().getPort());
        } catch (final IOException e) {
            this.running.set(false);
            throw new RuntimeException("Failed to start server on " + host + ":" + port, e);
        }
    }

    @Override
    public void stop() {
        if (this.running.compareAndSet(true, false)) {
            if (this.httpServer != null) {
                this.httpServer.stop(STOP_DELAY_SECONDS);
                this.httpServer = null;
            }
            if (this.executor != null) {
                this.executor.close();
                this.executor = null;
            }
            this.boundPort.set(-1);
        }
    }

    @Override
    public int port() {
        return this.boundPort.get();
    }

    @Override
    public boolean isRunning() {
        return this.running.get();
    }

    private void handleRequest(final HttpExchange exchange) {
        try {
            final Context context = createContext(exchange);
            this.handler.handle(context);
        } catch (final Exception e) {
            handleError(exchange, e);
        } finally {
            exchange.close();
        }
    }

    private Context createContext(final HttpExchange exchange) throws IOException {
        final DefaultContext.Request request = new ExchangeRequest(exchange);
        final DefaultContext.Response response = new ExchangeResponse(exchange);
        return new DefaultContext(request, response, this.jsonCodec);
    }

    private void handleError(final HttpExchange exchange, final Exception e) {
        try {
            final String message = e.getMessage() != null ? e.getMessage() : "Internal Server Error";
            final byte[] body = message.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(500, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        } catch (final IOException ignored) {
            // Response already started or connection closed
        }
    }

    /**
     * Adapter that bridges HttpExchange to DefaultContext.Request.
     */
    private static final class ExchangeRequest implements DefaultContext.Request {

        private final HttpExchange exchange;
        private final String path;
        private final Map<String, String> queryParams;
        private final Map<String, String> headers;
        private final byte[] body;
        private Map<String, String> params;

        ExchangeRequest(final HttpExchange exchange) throws IOException {
            this.exchange = exchange;
            this.params = new HashMap<>();

            final String uri = exchange.getRequestURI().toString();
            final int queryIndex = uri.indexOf('?');
            this.path = queryIndex >= 0 ? uri.substring(0, queryIndex) : uri;
            this.queryParams = parseQueryParams(queryIndex >= 0 ? uri.substring(queryIndex + 1) : "");
            this.headers = extractHeaders(exchange);
            this.body = readBody(exchange);
        }

        @Override
        public String method() {
            return this.exchange.getRequestMethod();
        }

        @Override
        public String path() {
            return this.path;
        }

        @Override
        public Map<String, String> params() {
            return this.params;
        }

        @Override
        public void setParams(final Map<String, String> params) {
            this.params = params != null ? params : new HashMap<>();
        }

        @Override
        public Map<String, String> queryParams() {
            return this.queryParams;
        }

        @Override
        public Map<String, String> headers() {
            return this.headers;
        }

        @Override
        public byte[] body() {
            return this.body;
        }

        private static Map<String, String> parseQueryParams(final String query) {
            if (query == null || query.isEmpty()) {
                return Map.of();
            }

            final Map<String, String> params = new HashMap<>();
            for (final String pair : query.split("&")) {
                final int eq = pair.indexOf('=');
                if (eq > 0) {
                    final String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                    final String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                    params.put(key, value);
                }
            }
            return Map.copyOf(params);
        }

        private static Map<String, String> extractHeaders(final HttpExchange exchange) {
            final Map<String, String> headers = new HashMap<>();
            exchange.getRequestHeaders().forEach((name, values) -> {
                if (values != null && !values.isEmpty()) {
                    headers.put(name.toLowerCase(), values.getFirst());
                }
            });
            return Map.copyOf(headers);
        }

        private static byte[] readBody(final HttpExchange exchange) throws IOException {
            try (InputStream in = exchange.getRequestBody()) {
                return in.readAllBytes();
            }
        }
    }

    /**
     * Adapter that bridges DefaultContext.Response to HttpExchange.
     */
    private static final class ExchangeResponse implements DefaultContext.Response {

        private final HttpExchange exchange;
        private int statusCode = 200;
        private boolean headersSent = false;

        ExchangeResponse(final HttpExchange exchange) {
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
            if (this.headersSent) {
                return;
            }
            this.headersSent = true;

            try {
                final int length = data != null ? data.length : 0;
                this.exchange.sendResponseHeaders(this.statusCode, length);
                if (length > 0) {
                    try (OutputStream out = this.exchange.getResponseBody()) {
                        out.write(data);
                    }
                }
            } catch (final IOException e) {
                throw new RuntimeException("Failed to send response", e);
            }
        }
    }
}
