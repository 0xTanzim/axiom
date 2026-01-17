package io.axiom.core.server;

import io.axiom.core.handler.*;

/**
 * HTTP server abstraction for Axiom.
 *
 * <p>
 * This interface represents a runnable HTTP server. Implementations
 * bridge between Axiom's Handler model and actual HTTP server libraries.
 *
 * <h2>Lifecycle</h2>
 *
 * <pre>
 * Server server = factory.create();
 * server.handler(composedHandler);
 * server.start(ServerConfig.defaults());
 * // ... server is running ...
 * server.stop();
 * </pre>
 *
 * <h2>Virtual Threads</h2>
 *
 * <p>
 * Implementations targeting Java 21+ SHOULD use virtual threads
 * for request handling via {@code Executors.newVirtualThreadPerTaskExecutor()}.
 *
 * @since 0.1.0
 */
public interface Server {

    /**
     * Sets the handler for incoming requests.
     *
     * <p>
     * Must be called before {@link #start(ServerConfig)}.
     *
     * @param handler the composed handler from App
     * @return this server for chaining
     * @throws NullPointerException if handler is null
     */
    Server handler(Handler handler);

    /**
     * Starts the server with the given configuration.
     *
     * <p>
     * This method blocks until the server is ready to accept connections.
     *
     * @param config the server configuration
     * @throws IllegalStateException if handler not set or already started
     */
    void start(ServerConfig config);

    /**
     * Starts the server on the specified host and port with default settings.
     *
     * <p>
     * Convenience method equivalent to:
     * <pre>{@code
     * start(ServerConfig.builder().host(host).port(port).build());
     * }</pre>
     *
     * @param host the host to bind to (e.g., "0.0.0.0" or "localhost")
     * @param port the port to listen on (0 for random available port)
     * @throws IllegalStateException if handler not set or already started
     */
    default void start(final String host, final int port) {
        this.start(ServerConfig.builder().host(host).port(port).build());
    }

    /**
     * Stops the server gracefully.
     *
     * <p>
     * Waits for in-flight requests to complete up to the configured timeout.
     */
    void stop();

    /**
     * Returns the actual port the server is listening on.
     *
     * <p>
     * Useful when port 0 was specified to get an auto-assigned port.
     *
     * @return the bound port, or -1 if not started
     */
    int port();

    /**
     * Returns whether the server is currently running.
     *
     * @return true if started and not stopped
     */
    boolean isRunning();
}
