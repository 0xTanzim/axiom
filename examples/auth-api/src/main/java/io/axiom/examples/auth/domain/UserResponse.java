package io.axiom.examples.auth.domain;

/**
 * User response DTO (excludes password hash).
 */
public record UserResponse(
    long id,
    String username,
    String email,
    long createdAt
) {}
