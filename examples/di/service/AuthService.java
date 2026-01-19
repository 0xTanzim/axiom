package playground.di.service;

import io.axiom.di.Service;
import jakarta.inject.Inject;
import playground.di.domain.*;
import playground.di.repository.UserRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication service.
 *
 * Handles:
 * - User login (email/password → token)
 * - User registration
 * - Token validation
 * - Logout (token invalidation)
 */
@Service
public class AuthService {

    private final UserRepository userRepository;

    // In-memory token store (use Redis in production)
    private final Map<String, Long> tokenToUserId = new ConcurrentHashMap<>();

    @Inject
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Authenticate user and return token.
     */
    public AuthResult login(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return new AuthResult.Failure("Invalid email or password");
        }

        User user = userOpt.get();
        String hashedPassword = hashPassword(password);

        if (!user.passwordHash().equals(hashedPassword)) {
            return new AuthResult.Failure("Invalid email or password");
        }

        // Generate token
        String token = generateToken(user.id());
        tokenToUserId.put(token, user.id());

        return new AuthResult.Success(TokenResponse.bearer(token, user));
    }

    /**
     * Register new user.
     */
    public AuthResult register(RegisterRequest request) {
        // Check for duplicate email
        if (userRepository.existsByEmail(request.email())) {
            return new AuthResult.Failure("Email already registered", true);
        }

        // Create user
        User user = User.create(
            request.name(),
            request.email(),
            hashPassword(request.password())
        );
        userRepository.save(user);

        // Generate token
        String token = generateToken(user.id());
        tokenToUserId.put(token, user.id());

        return new AuthResult.Success(TokenResponse.bearer(token, user));
    }

    /**
     * Validate token and return user.
     */
    public Optional<User> validateToken(String token) {
        Long userId = tokenToUserId.get(token);
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId);
    }

    /**
     * Invalidate token (logout).
     */
    public void logout(String token) {
        tokenToUserId.remove(token);
    }

    private String generateToken(long userId) {
        return UUID.randomUUID().toString() + "-" + userId;
    }

    private String hashPassword(String password) {
        // Simple hash for demo - use BCrypt in production!
        return Integer.toHexString(password.hashCode());
    }

    // ===========================================
    // Result Types
    // ===========================================

    public sealed interface AuthResult {
        record Success(TokenResponse token) implements AuthResult {}
        record Failure(String message, boolean isConflict) implements AuthResult {
            public Failure(String message) {
                this(message, false);
            }
        }
    }
}
