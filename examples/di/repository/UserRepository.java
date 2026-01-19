package playground.di.repository;

import io.axiom.di.Repository;
import jakarta.inject.Inject;
import playground.di.domain.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory user repository.
 *
 * In production, this would be backed by a database using:
 * - AxiomPersistence + JDBC
 * - jOOQ
 * - JPA
 */
@Repository
public class UserRepository {

    private final Map<Long, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, User> usersByEmail = new ConcurrentHashMap<>();

    @Inject
    public UserRepository() {
        // Seed demo user
        save(User.create("Demo User", "demo@example.com", hashPassword("password123")));
    }

    public User save(User user) {
        usersById.put(user.id(), user);
        usersByEmail.put(user.email().toLowerCase(), user);
        return user;
    }

    public Optional<User> findById(long id) {
        return Optional.ofNullable(usersById.get(id));
    }

    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(usersByEmail.get(email.toLowerCase()));
    }

    public List<User> findAll() {
        return List.copyOf(usersById.values());
    }

    public boolean existsByEmail(String email) {
        return usersByEmail.containsKey(email.toLowerCase());
    }

    public long count() {
        return usersById.size();
    }

    private String hashPassword(String password) {
        // Simple hash for demo - use BCrypt in production!
        return Integer.toHexString(password.hashCode());
    }
}
