package io.axiom.examples.auth;

import io.axiom.di.AxiomApplication;

/**
 * Main application entry point.
 *
 * <p>ONE LINE startup. That's it.
 * Framework handles everything:
 * <ul>
 *   <li>Discovers @Service, @Repository, @Routes in this package</li>
 *   <li>Wires dependencies automatically</li>
 *   <li>Mounts all @Routes automatically</li>
 *   <li>Starts server</li>
 * </ul>
 *
 * <h2>Running</h2>
 * <pre>
 * cd examples/auth-api
 * mvn compile exec:java
 * </pre>
 *
 * <h2>Test the API</h2>
 * <pre>
 * curl http://localhost:8080/health
 * curl http://localhost:8080/users
 * curl -X POST http://localhost:8080/auth/login \
 *   -H "Content-Type: application/json" \
 *   -d '{"username":"demo","password":"demo123"}'
 * </pre>
 */
public class Application {

    public static void main(String[] args) {
        // ONE LINE. Framework handles discovery, DI, and mounting.
        AxiomApplication.start(Application.class, 8080);
    }
}
