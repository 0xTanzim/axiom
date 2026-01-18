package io.axiom.examples.auth.service;

import io.axiom.di.Service;
import io.axiom.examples.auth.domain.*;
import io.axiom.examples.auth.repository.UserRepository;
import jakarta.inject.Inject;

import java.util.*;

/**
 * Authentication service handling login, registration, and token management.
 *
 * <p>Demonstrates Axiom DI pattern with @Service and @Inject.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final Map<String, Long> tokenStore = new HashMap<>();  // token -> userId

    @Inject
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Authenticates user and returns auth token.
     */
    public AuthResult login(LoginRequest request) {
        if (!request.isValid()) {
            return AuthResult.failure("Username and password are required");
        }

        var userOpt = userRepository.findByUsername(request.username());
        if (userOpt.isEmpty()) {
            return AuthResult.failure("Invalid username or password");
        }

        var user = userOpt.get();
        if (!verifyPassword(request.password(), user.passwordHash())) {
            return AuthResult.failure("Invalid username or password");
        }

        var token = generateToken(user.id());
        var expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 hours
        tokenStore.put(token, user.id());

        return AuthResult.success(new AuthToken(token, expiresAt, user.toResponse()));
    }

    /**
     * Registers a new user.
     */
    public AuthResult register(RegisterRequest request) {
        if (!request.isValid()) {
            return AuthResult.failure("Invalid registration data");
        }

        if (userRepository.existsByUsername(request.username())) {
            return AuthResult.conflict("Username already exists");
        }

        if (userRepository.existsByEmail(request.email())) {
            return AuthResult.conflict("Email already exists");
        }

        var passwordHash = hashPassword(request.password());
        var user = User.create(request.username(), request.email(), passwordHash);
        userRepository.save(user);

        var token = generateToken(user.id());
        var expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
        tokenStore.put(token, user.id());

        return AuthResult.success(new AuthToken(token, expiresAt, user.toResponse()));
    }

    /**
     * Validates token and returns associated user.
     */
    public Optional<User> validateToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        var userId = tokenStore.get(token);
        if (userId == null) {
            return Optional.empty();
        }

        return userRepository.findById(userId);
    }

    /**
     * Invalidates a token (logout).
     */
    public void logout(String token) {
        tokenStore.remove(token);
    }

    private String generateToken(long userId) {
        return UUID.randomUUID().toString() + "-" + Long.toHexString(userId);
    }

    private String hashPassword(String password) {
        // Simple hash for demo - use BCrypt in production!
        return Integer.toHexString(password.hashCode());
    }

    private boolean verifyPassword(String password, String hash) {
        return hashPassword(password).equals(hash);
    }

    /**
     * Result of auth operation.
     */
    public sealed interface AuthResult {
        record Success(AuthToken token) implements AuthResult {}
        record Failure(String message, boolean isConflict) implements AuthResult {}

        static AuthResult success(AuthToken token) {
            return new Success(token);
        }

        static AuthResult failure(String message) {
            return new Failure(message, false);
        }

        static AuthResult conflict(String message) {
            return new Failure(message, true);
        }
    }
}
