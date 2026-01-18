package io.axiom.di;

import java.lang.annotation.*;

import jakarta.inject.*;

/**
 * Marks a class as a service component for dependency injection.
 *
 * <p>Services are the primary business logic components in an Axiom application.
 * They are automatically discovered and wired by the DI container.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * @Service
 * public class UserService {
 *     private final UserRepository repository;
 *
 *     @Inject
 *     public UserService(UserRepository repository) {
 *         this.repository = repository;
 *     }
 *
 *     public User findById(Long id) {
 *         return repository.findById(id).orElseThrow();
 *     }
 * }
 * }</pre>
 *
 * <h2>Lifecycle</h2>
 * <p>Services are singletons by default. They are created once during
 * application startup and reused for all requests.
 *
 * <h2>DI Integration</h2>
 * <p>This annotation is meta-annotated with {@code @Singleton} for Dagger 2
 * integration. Use {@code @Inject} on constructors for dependency injection.
 *
 * @see Repository
 * @see Routes
 * @see jakarta.inject.Inject
 * @since 0.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Singleton  // Dagger integration: services are singletons
public @interface Service {

    /**
     * Optional name for the service.
     *
     * <p>Used for qualification when multiple implementations exist.
     * Default is empty (use class name).
     *
     * @return the service name
     */
    String value() default "";
}
