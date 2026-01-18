package io.axiom.examples.auth.domain;

/**
 * User domain entity.
 *
 * <p>Immutable record representing a user in the system.
 * Uses Java 25 record for clean, explicit data modeling.
 */
public record User(
    long id,
    String username,
    String email,
    String passwordHash,
    long createdAt
) {
    /**
     * Creates a new user with generated ID and timestamp.
     */
    public static User create(String username, String email, String passwordHash) {
        return new User(
            System.nanoTime(),  // Simple ID generation for demo
            username,
            email,
            passwordHash,
            System.currentTimeMillis()
        );
    }

    /**
     * Returns user without sensitive data (for API responses).
     */
    public UserResponse toResponse() {
        return new UserResponse(id, username, email, createdAt);
    }
}
