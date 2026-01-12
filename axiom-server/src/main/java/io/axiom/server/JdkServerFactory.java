package io.axiom.server;

import io.axiom.core.server.Server;
import io.axiom.core.server.ServerFactory;

/**
 * JDK HttpServer factory implementation.
 *
 * <p>
 * Provides the default Axiom server runtime using Java's built-in
 * {@code com.sun.net.httpserver.HttpServer} with virtual threads.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Zero external dependencies (uses JDK only)</li>
 *   <li>Virtual threads for massive concurrency (Java 21+)</li>
 *   <li>Automatic service discovery via JPMS</li>
 * </ul>
 *
 * <h2>Virtual Threads</h2>
 * <p>
 * Each incoming request is handled on its own virtual thread,
 * enabling millions of concurrent connections without thread pool tuning.
 * Java 25 removes synchronized-block pinning (JEP 491), making
 * virtual threads work seamlessly with legacy code.
 *
 * @since 0.1.0
 */
public final class JdkServerFactory implements ServerFactory {

    private static final int DEFAULT_PRIORITY = 0;

    @Override
    public Server create() {
        return new JdkServer();
    }

    @Override
    public int priority() {
        return DEFAULT_PRIORITY;
    }

    @Override
    public String name() {
        return "jdk-httpserver";
    }
}
