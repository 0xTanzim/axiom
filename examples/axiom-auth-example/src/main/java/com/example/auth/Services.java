package com.example.auth;

import com.example.auth.config.AppConfig;
import com.example.auth.middleware.AuthMiddleware;
import com.example.auth.repository.UserRepository;
import com.example.auth.routes.AuthRoutes;
import com.example.auth.routes.UserRoutes;
import com.example.auth.service.AuthService;
import com.example.auth.service.JwtService;

/**
 * Application services container.
 *
 * <p>This is the recommended pattern for dependency injection in Axiom:
 * <ul>
 *   <li>All service creation in one place</li>
 *   <li>Clear dependency graph (visible in IDE)</li>
 *   <li>Compile-time type safety</li>
 *   <li>Easy to test (just pass mocks)</li>
 *   <li>No reflection, no magic</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * var services = new Services(appConfig);
 * app.route("/auth", services.authRoutes.router());
 * }</pre>
 *
 * <h2>Testing</h2>
 * <pre>{@code
 * // Create with mocks
 * var mockRepo = new MockUserRepository();
 * var services = new Services(mockRepo, testConfig);
 * }</pre>
 */
public class Services {

    // Repositories
    public final UserRepository userRepository;

    // Services
    public final JwtService jwtService;
    public final AuthService authService;

    // Routes
    public final AuthRoutes authRoutes;
    public final UserRoutes userRoutes;

    // Middleware
    public final AuthMiddleware authMiddleware;

    /**
     * Creates all application services with proper wiring.
     *
     * @param config application configuration
     */
    public Services(AppConfig config) {
        // Layer 1: Repositories (no dependencies)
        this.userRepository = new UserRepository();

        // Layer 2: Services (depend on repositories + config)
        this.jwtService = new JwtService(config.jwt());
        this.authService = new AuthService(userRepository, jwtService);

        // Layer 3: Routes (depend on services)
        this.authRoutes = new AuthRoutes(authService);
        this.userRoutes = new UserRoutes(authService);

        // Layer 4: Middleware (depend on services)
        this.authMiddleware = new AuthMiddleware(jwtService);
    }

    /**
     * Constructor for testing with custom repository.
     */
    public Services(UserRepository userRepository, AppConfig config) {
        this.userRepository = userRepository;
        this.jwtService = new JwtService(config.jwt());
        this.authService = new AuthService(userRepository, jwtService);
        this.authRoutes = new AuthRoutes(authService);
        this.userRoutes = new UserRoutes(authService);
        this.authMiddleware = new AuthMiddleware(jwtService);
    }
}
