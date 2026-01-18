package io.axiom.examples.auth.domain;

/**
 * Authentication token response.
 */
public record AuthToken(
    String token,
    long expiresAt,
    UserResponse user
) {}
