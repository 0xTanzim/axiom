package playground.di.domain;

/**
 * User entity.
 */
public record User(
    long id,
    String name,
    String email,
    String passwordHash,
    long createdAt
) {
    public static User create(String name, String email, String passwordHash) {
        return new User(
            System.nanoTime(),  // Simple ID generation for demo
            name,
            email,
            passwordHash,
            System.currentTimeMillis()
        );
    }

    public UserResponse toResponse() {
        return new UserResponse(id, name, email, createdAt);
    }
}
