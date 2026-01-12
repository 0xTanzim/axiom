package io.axiom.core.server;

/**
 * Service Provider Interface (SPI) for HTTP server implementations.
 *
 * <p>
 * This interface allows different HTTP server runtimes to be plugged
 * into Axiom without coupling core to any specific implementation.
 * Implementations are discovered via {@link java.util.ServiceLoader}.
 *
 * <h2>Architecture</h2>
 *
 * <pre>
 * axiom-core (contract)
 *   ↑
 *   └── ServerFactory (SPI)
 *
 * axiom-server (implementation)
 *   ↓
 *   └── JdkServerFactory implements ServerFactory
 * </pre>
 *
 * <h2>Service Discovery</h2>
 *
 * Implementations must:
 * <ol>
 *   <li>Implement this interface</li>
 *   <li>Provide {@code META-INF/services/io.axiom.core.server.ServerFactory}</li>
 *   <li>Or use JPMS {@code provides ... with ...} in module-info</li>
 * </ol>
 *
 * @since 0.1.0
 */
public interface ServerFactory {

    /**
     * Creates a new server instance.
     *
     * @return a new server ready to be configured
     */
    Server create();

    /**
     * Returns the priority of this factory.
     *
     * <p>
     * When multiple factories are available, the one with
     * the highest priority is selected. Default is 0.
     *
     * @return priority value (higher = preferred)
     */
    default int priority() {
        return 0;
    }

    /**
     * Returns the name of this server implementation.
     *
     * @return human-readable name (e.g., "JDK HttpServer", "Netty")
     */
    String name();
}
