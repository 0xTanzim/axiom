package com.example.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.auth.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;

/**
 * JWT token generation and validation service.
 *
 * <p>Uses Auth0's java-jwt library for standards-compliant JWT handling.
 */
public class JwtService {

    private static final Logger LOG = LoggerFactory.getLogger(JwtService.class);

    private final Algorithm algorithm;
    private final String issuer;
    private final long expirationSeconds;

    public JwtService(AppConfig.JwtConfig config) {
        this.algorithm = Algorithm.HMAC256(config.secret());
        this.issuer = config.issuer();
        this.expirationSeconds = config.expiration();
    }

    /**
     * Generates a JWT token for the given user ID.
     *
     * @param userId the user's ID
     * @return the signed JWT token
     */
    public String generateToken(Long userId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationSeconds);

        String token = JWT.create()
                .withIssuer(issuer)
                .withSubject(userId.toString())
                .withIssuedAt(now)
                .withExpiresAt(expiry)
                .sign(algorithm);

        LOG.debug("Generated token for user={}, expires={}", userId, expiry);
        return token;
    }

    /**
     * Validates a JWT token and extracts the user ID.
     *
     * @param token the JWT token
     * @return the user ID if valid, empty otherwise
     */
    public Optional<Long> validateToken(String token) {
        try {
            DecodedJWT decoded = JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build()
                    .verify(token);

            Long userId = Long.parseLong(decoded.getSubject());
            LOG.debug("Validated token for user={}", userId);
            return Optional.of(userId);

        } catch (JWTVerificationException e) {
            LOG.debug("Token validation failed: {}", e.getMessage());
            return Optional.empty();
        } catch (NumberFormatException e) {
            LOG.warn("Invalid subject in token: not a valid user ID");
            return Optional.empty();
        }
    }

    /**
     * Returns the token expiration time in seconds.
     */
    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
