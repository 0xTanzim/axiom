/**
 * Axiom DI module - Compile-time dependency injection powered by Dagger 2.
 *
 * <p>This module provides:
 * <ul>
 *   <li>{@code @Service} - Business logic components</li>
 *   <li>{@code @Repository} - Data access components</li>
 *   <li>{@code @Routes} - HTTP route handlers with auto-mounting</li>
 *   <li>{@code @Middleware} - Request interceptors</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * public class Application {
 *     public static void main(String[] args) {
 *         AxiomApplication.start(8080);
 *     }
 * }
 * }</pre>
 *
 * <h2>Annotations Usage</h2>
 * <pre>{@code
 * import io.axiom.di.*;
 * import jakarta.inject.Inject;  // For @Inject
 *
 * @Service
 * public class UserService {
 *     @Inject
 *     public UserService(UserRepository repo) { ... }
 * }
 * }</pre>
 */
module io.axiom.di {
    requires io.axiom.core;
    requires org.slf4j;
    requires dagger;
    requires transitive jakarta.inject;  // Re-export so users can use @Inject

    exports io.axiom.di;
}
