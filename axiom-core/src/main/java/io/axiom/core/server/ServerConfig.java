package io.axiom.core.server;

import java.time.*;
import java.util.*;

/**
 * Configuration for HTTP server runtime.
 *
 * <p>
 * Immutable configuration record with builder pattern.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ServerConfig config = ServerConfig.builder()
 *     .host("0.0.0.0")
 *     .port(8080)
 *     .maxRequestSize(10 * 1024 * 1024)  // 10MB
 *     .shutdownTimeout(Duration.ofSeconds(30))
 *     .build();
 * }</pre>
 *
 * @since 0.1.0
 */
public record ServerConfig(
        String host,
        int port,
        int maxRequestSize,
        Duration readTimeout,
        Duration writeTimeout,
        Duration shutdownTimeout,
        Duration drainTimeout,
        boolean virtualThreads
) {

    /**
     * Default host: all interfaces.
     */
    public static final String DEFAULT_HOST = "0.0.0.0";

    /**
     * Default port.
     */
    public static final int DEFAULT_PORT = 8080;

    /**
     * Default max request body size: 10MB.
     */
    public static final int DEFAULT_MAX_REQUEST_SIZE = 10 * 1024 * 1024;

    /**
     * Default read timeout: 30 seconds.
     */
    public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Default write timeout: 30 seconds.
     */
    public static final Duration DEFAULT_WRITE_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Default shutdown timeout: 30 seconds.
     */
    public static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Default drain timeout: 10 seconds.
     */
    public static final Duration DEFAULT_DRAIN_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Canonical constructor with validation.
     */
    public ServerConfig {
        Objects.requireNonNull(host, "host cannot be null");
        Objects.requireNonNull(readTimeout, "readTimeout cannot be null");
        Objects.requireNonNull(writeTimeout, "writeTimeout cannot be null");
        Objects.requireNonNull(shutdownTimeout, "shutdownTimeout cannot be null");
        Objects.requireNonNull(drainTimeout, "drainTimeout cannot be null");

        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be 0-65535, got: " + port);
        }
        if (maxRequestSize <= 0) {
            throw new IllegalArgumentException("maxRequestSize must be positive, got: " + maxRequestSize);
        }
    }

    /**
     * Returns default configuration.
     *
     * @return default config
     */
    public static ServerConfig defaults() {
        return new ServerConfig(
                ServerConfig.DEFAULT_HOST,
                ServerConfig.DEFAULT_PORT,
                ServerConfig.DEFAULT_MAX_REQUEST_SIZE,
                ServerConfig.DEFAULT_READ_TIMEOUT,
                ServerConfig.DEFAULT_WRITE_TIMEOUT,
                ServerConfig.DEFAULT_SHUTDOWN_TIMEOUT,
                ServerConfig.DEFAULT_DRAIN_TIMEOUT,
                true
        );
    }

    /**
     * Creates a new builder with default values.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ServerConfig.
     */
    public static final class Builder {
        private String host = ServerConfig.DEFAULT_HOST;
        private int port = ServerConfig.DEFAULT_PORT;
        private int maxRequestSize = ServerConfig.DEFAULT_MAX_REQUEST_SIZE;
        private Duration readTimeout = ServerConfig.DEFAULT_READ_TIMEOUT;
        private Duration writeTimeout = ServerConfig.DEFAULT_WRITE_TIMEOUT;
        private Duration shutdownTimeout = ServerConfig.DEFAULT_SHUTDOWN_TIMEOUT;
        private Duration drainTimeout = ServerConfig.DEFAULT_DRAIN_TIMEOUT;
        private boolean virtualThreads = true;

        private Builder() {
        }

        public Builder host(final String host) {
            this.host = host;
            return this;
        }

        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        public Builder maxRequestSize(final int maxRequestSize) {
            this.maxRequestSize = maxRequestSize;
            return this;
        }

        public Builder readTimeout(final Duration readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder writeTimeout(final Duration writeTimeout) {
            this.writeTimeout = writeTimeout;
            return this;
        }

        public Builder shutdownTimeout(final Duration shutdownTimeout) {
            this.shutdownTimeout = shutdownTimeout;
            return this;
        }

        public Builder drainTimeout(final Duration drainTimeout) {
            this.drainTimeout = drainTimeout;
            return this;
        }

        public Builder virtualThreads(final boolean virtualThreads) {
            this.virtualThreads = virtualThreads;
            return this;
        }

        public ServerConfig build() {
            return new ServerConfig(
                    this.host,
                    this.port,
                    this.maxRequestSize,
                    this.readTimeout,
                    this.writeTimeout,
                    this.shutdownTimeout,
                    this.drainTimeout,
                    this.virtualThreads
            );
        }
    }
}
