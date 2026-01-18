package io.axiom.di;

import java.lang.annotation.*;

import jakarta.inject.*;

/**
 * Marks a class as an HTTP routes handler with auto-mounting.
 *
 * <p>Routes classes define HTTP endpoints. When the path is specified,
 * they are automatically mounted to that path during application startup.
 *
 * <h2>Auto-Mount Usage</h2>
 * <pre>{@code
 * @Routes("/users")  // Auto-mounted at /users
 * public class UserRoutes {
 *     private final UserService userService;
 *
 *     @Inject
 *     public UserRoutes(UserService userService) {
 *         this.userService = userService;
 *     }
 *
 *     public Router router() {
 *         Router r = new Router();
 *         r.get("/", c -> c.json(userService.findAll()));
 *         r.get("/:id", c -> c.json(userService.findById(c.paramLong("id"))));
 *         r.post("/", c -> c.json(userService.create(c.body(User.class))));
 *         return r;
 *     }
 * }
 * }</pre>
 *
 * <h2>Manual Mount (RFC-0002 Compatible)</h2>
 * <p>You can still use manual mounting if you need more control:
 * <pre>{@code
 * // In your Application.java
 * app.route("/admin", adminRoutes::router);
 * app.route("/health", healthRoutes::router);
 * }</pre>
 *
 * <h2>DI Integration</h2>
 * <p>This annotation is meta-annotated with {@code @Singleton} for Dagger 2
 * integration. Use {@code @Inject} on constructors for dependency injection.
 *
 * @see Service
 * @see Repository
 * @see jakarta.inject.Inject
 * @since 0.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Singleton  // Dagger integration: routes are singletons
public @interface Routes {

    /**
     * The base path for this routes handler.
     *
     * <p>When specified, the routes are automatically mounted at this path.
     * If empty, routes must be manually mounted via {@code app.route()}.
     *
     * <p>Example: {@code @Routes("/api/users")} mounts at /api/users
     *
     * @return the base path, or empty for manual mounting
     */
    String value() default "";

    /**
     * Order priority for mounting (lower = earlier).
     *
     * <p>Use this to control the order in which routes are mounted.
     * Default is 0. Use negative values for earlier mounting.
     *
     * @return the mount order priority
     */
    int order() default 0;
}
