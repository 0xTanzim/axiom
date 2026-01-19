package playground.di.domain;

/**
 * User response DTO (without password hash).
 */
public record UserResponse(
    long id,
    String name,
    String email,
    long createdAt
) {}
