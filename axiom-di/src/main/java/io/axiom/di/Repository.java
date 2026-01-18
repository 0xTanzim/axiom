package io.axiom.di;

import java.lang.annotation.*;

import jakarta.inject.*;

/**
 * Marks a class as a repository component for data access.
 *
 * <p>Repositories handle data persistence and retrieval. They are automatically
 * discovered and wired by the DI container with transaction awareness.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * @Repository
 * public class UserRepository {
 *     private final DataSource dataSource;
 *
 *     @Inject
 *     public UserRepository(DataSource dataSource) {
 *         this.dataSource = dataSource;
 *     }
 *
 *     public Optional<User> findById(Long id) {
 *         return Ax.query("SELECT * FROM users WHERE id = ?")
 *             .param(id)
 *             .mapTo(User.class)
 *             .findFirst();
 *     }
 * }
 * }</pre>
 *
 * <h2>Transaction Awareness</h2>
 * <p>Repositories marked with this annotation can participate in
 * transactions managed by {@code @Transactional} services.
 *
 * <h2>DI Integration</h2>
 * <p>This annotation is meta-annotated with {@code @Singleton} for Dagger 2
 * integration. Use {@code @Inject} on constructors for dependency injection.
 *
 * @see Service
 * @see Routes
 * @see jakarta.inject.Inject
 * @since 0.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Singleton  // Dagger integration: repositories are singletons
public @interface Repository {

    /**
     * Optional name for the repository.
     *
     * @return the repository name
     */
    String value() default "";
}
