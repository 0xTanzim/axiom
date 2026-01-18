package io.axiom.examples.auth.repository;

import io.axiom.di.Repository;
import io.axiom.examples.auth.domain.User;
import jakarta.inject.Inject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory user repository for demonstration.
 *
 * <p>Uses ConcurrentHashMap for thread-safe access.
 * In production, this would be backed by a database.
 */
@Repository
public class UserRepository {

    private final Map<Long, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    private final Map<String, User> usersByEmail = new ConcurrentHashMap<>();

    @Inject
    public UserRepository() {
        // Seed with demo user
        var demo = User.create("demo", "demo@example.com", hashPassword("demo123"));
        save(demo);
    }

    public User save(User user) {
        usersById.put(user.id(), user);
        usersByUsername.put(user.username().toLowerCase(), user);
        usersByEmail.put(user.email().toLowerCase(), user);
        return user;
    }

    public Optional<User> findById(long id) {
        return Optional.ofNullable(usersById.get(id));
    }

    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username.toLowerCase()));
    }

    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(usersByEmail.get(email.toLowerCase()));
    }

    public List<User> findAll() {
        return List.copyOf(usersById.values());
    }

    public boolean existsByUsername(String username) {
        return usersByUsername.containsKey(username.toLowerCase());
    }

    public boolean existsByEmail(String email) {
        return usersByEmail.containsKey(email.toLowerCase());
    }

    public void deleteById(long id) {
        var user = usersById.remove(id);
        if (user != null) {
            usersByUsername.remove(user.username().toLowerCase());
            usersByEmail.remove(user.email().toLowerCase());
        }
    }

    public long count() {
        return usersById.size();
    }

    // Simple password hashing (use BCrypt in production!)
    private String hashPassword(String password) {
        return Integer.toHexString(password.hashCode());
    }
}
