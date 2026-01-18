package com.example.auth.dto;

/**
 * Authentication response with JWT token.
 */
public record AuthResponse(
        String token,
        String type,
        long expiresIn,
        UserInfo user
) {
    public static AuthResponse of(String token, long expiresIn, UserInfo user) {
        return new AuthResponse(token, "Bearer", expiresIn, user);
    }

    /**
     * Minimal user info returned after authentication.
     */
    public record UserInfo(
            Long id,
            String email,
            String name
    ) {
    }
}
