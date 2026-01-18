package com.example.auth.domain;

import java.time.Instant;

/**
 * User entity.
 *
 * <p>Using a Java record for immutability. For mutable entities,
 * you could use a regular class with Lombok's @Data.
 */
public record User(
        Long id,
        String email,
        String passwordHash,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Creates a new user (before insert, id is null).
     */
    public static User create(String email, String passwordHash, String name) {
        Instant now = Instant.now();
        return new User(null, email, passwordHash, name, now, now);
    }

    /**
     * Creates a copy with an assigned ID (after insert).
     */
    public User withId(Long id) {
        return new User(id, email, passwordHash, name, createdAt, updatedAt);
    }

    /**
     * Creates a copy with updated fields.
     */
    public User withUpdates(String name) {
        return new User(id, email, passwordHash, name, createdAt, Instant.now());
    }
}
