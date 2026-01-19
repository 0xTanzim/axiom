package playground.di;

import io.axiom.di.AxiomApplication;

/**
 * DI Mode Application - ONE LINE startup.
 *
 * The framework automatically:
 * 1. Scans this package and subpackages
 * 2. Discovers @Service, @Repository, @Routes, @Middleware classes
 * 3. Instantiates them in dependency order
 * 4. Wires dependencies via @Inject constructors
 * 5. Mounts all @Routes to their paths
 * 6. Starts the server
 *
 * Run:
 *   mvn compile exec:java -Dexec.mainClass=playground.di.Application
 *
 * Test:
 *   curl http://localhost:8080/health
 *   curl http://localhost:8080/users
 *   curl http://localhost:8080/auth/login -X POST \
 *     -H "Content-Type: application/json" \
 *     -d '{"email":"demo@example.com","password":"password123"}'
 */
public class Application {

    public static void main(String[] args) {
        // ONE LINE — that's the entire application bootstrap
        AxiomApplication.start(Application.class, 8080);
    }
}
