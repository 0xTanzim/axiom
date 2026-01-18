package com.example.auth.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.example.auth.domain.User;
import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Authentication service handling registration and login.
 *
 * <p>Uses BCrypt for password hashing (cost factor 12).
 */
public class AuthService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);
    private static final int BCRYPT_COST = 12;

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new user.
     *
     * @param request the registration request
     * @return authentication response with token, or empty if email exists
     */
    public Optional<AuthResponse> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            LOG.info("Registration failed: email already exists: {}", request.email());
            return Optional.empty();
        }

        String passwordHash = hashPassword(request.password());
        User user = User.create(request.email(), passwordHash, request.name());
        User saved = userRepository.save(user);

        LOG.info("Registered new user: id={}, email={}", saved.id(), saved.email());
        return Optional.of(createAuthResponse(saved));
    }

    /**
     * Authenticates a user with email and password.
     *
     * @param request the login request
     * @return authentication response with token, or empty if invalid
     */
    public Optional<AuthResponse> login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.email());

        if (userOpt.isEmpty()) {
            LOG.debug("Login failed: user not found: {}", request.email());
            return Optional.empty();
        }

        User user = userOpt.get();

        if (!verifyPassword(request.password(), user.passwordHash())) {
            LOG.debug("Login failed: invalid password for: {}", request.email());
            return Optional.empty();
        }

        LOG.info("User logged in: id={}, email={}", user.id(), user.email());
        return Optional.of(createAuthResponse(user));
    }

    /**
     * Gets a user by ID (for authenticated requests).
     */
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * Updates a user's profile.
     */
    public Optional<User> updateUser(Long userId, String name) {
        return userRepository.findById(userId)
                .map(user -> user.withUpdates(name))
                .map(userRepository::update);
    }

    private AuthResponse createAuthResponse(User user) {
        String token = jwtService.generateToken(user.id());
        var userInfo = new AuthResponse.UserInfo(user.id(), user.email(), user.name());
        return AuthResponse.of(token, jwtService.getExpirationSeconds(), userInfo);
    }

    private String hashPassword(String password) {
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray());
    }

    private boolean verifyPassword(String password, String hash) {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified;
    }
}
