package io.axiom.di;

import java.lang.annotation.*;

import jakarta.inject.*;

/**
 * Marks a class as middleware for the HTTP pipeline.
 *
 * <p>Middleware classes intercept requests before they reach route handlers.
 * They are automatically discovered and wired by the DI container.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * @Middleware
 * public class AuthMiddleware {
 *     private final JwtService jwtService;
 *
 *     @Inject
 *     public AuthMiddleware(JwtService jwtService) {
 *         this.jwtService = jwtService;
 *     }
 *
 *     public void handle(Context ctx, Runnable next) {
 *         String token = ctx.header("Authorization");
 *         if (token == null) {
 *             ctx.status(401);
 *             ctx.json(Map.of("error", "Unauthorized"));
 *             return;
 *         }
 *         // Validate token and set user
 *         ctx.set("user", jwtService.validateToken(token));
 *         next.run();
 *     }
 * }
 * }</pre>
 *
 * <h2>Using in Routes</h2>
 * <pre>{@code
 * @Routes("/users")
 * public class UserRoutes {
 *     private final UserService userService;
 *     private final AuthMiddleware auth;
 *
 *     @Inject
 *     public UserRoutes(UserService userService, AuthMiddleware auth) {
 *         this.userService = userService;
 *         this.auth = auth;
 *     }
 *
 *     public Router router() {
 *         Router r = new Router();
 *         r.use(auth::handle);  // Apply middleware
 *         r.get("/me", c -> c.json(c.get("user")));
 *         return r;
 *     }
 * }
 * }</pre>
 *
 * <h2>DI Integration</h2>
 * <p>This annotation is meta-annotated with {@code @Singleton} for Dagger 2
 * integration. Use {@code @Inject} on constructors for dependency injection.
 *
 * @see Routes
 * @see Service
 * @see jakarta.inject.Inject
 * @since 0.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Singleton  // Dagger integration: middleware are singletons
public @interface Middleware {

    /**
     * Optional name for the middleware.
     *
     * @return the middleware name
     */
    String value() default "";

    /**
     * Whether this middleware is global (applies to all routes).
     *
     * <p>Global middleware is automatically applied to all routes.
     * Non-global middleware must be explicitly used in route handlers.
     *
     * @return true if global, false if explicit
     */
    boolean global() default false;

    /**
     * Order priority for global middleware (lower = earlier).
     *
     * @return the order priority
     */
    int order() default 0;
}
