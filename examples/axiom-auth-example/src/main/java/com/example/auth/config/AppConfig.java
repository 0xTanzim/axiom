package com.example.auth.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Type-safe application configuration.
 *
 * <p>Uses SmallRye Config's {@code @ConfigMapping} for compile-time safety.
 * Values come from (in priority order):
 * <ol>
 *   <li>System properties: {@code -Dserver.port=9090}</li>
 *   <li>Environment variables: {@code SERVER_PORT=9090}</li>
 *   <li>.env file</li>
 *   <li>application.properties</li>
 * </ol>
 */
@ConfigMapping(prefix = "")
public interface AppConfig {

    /**
     * Server configuration.
     */
    ServerConfig server();

    /**
     * Database configuration.
     */
    DatabaseConfig database();

    /**
     * JWT configuration.
     */
    JwtConfig jwt();

    interface ServerConfig {
        @WithDefault("0.0.0.0")
        String host();

        @WithDefault("8080")
        int port();
    }

    interface DatabaseConfig {
        @WithDefault("jdbc:h2:mem:auth;DB_CLOSE_DELAY=-1")
        String url();

        @WithDefault("sa")
        String username();

        @WithDefault("")
        String password();
    }

    interface JwtConfig {
        String secret();

        @WithDefault("86400")
        long expiration();

        @WithDefault("axiom-auth")
        String issuer();
    }
}
