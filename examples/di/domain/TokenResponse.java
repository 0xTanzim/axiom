package playground.di.domain;

/**
 * Token response for authentication.
 */
public record TokenResponse(
    String token,
    String type,
    long expiresIn,
    UserResponse user
) {
    public static TokenResponse bearer(String token, User user) {
        return new TokenResponse(token, "Bearer", 3600, user.toResponse());
    }
}
