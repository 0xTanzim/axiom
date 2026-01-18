package io.axiom.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

/**
 * Test configuration mapping for server settings.
 */
@ConfigMapping(prefix = "server")
public interface ServerConfig {

    @WithDefault("localhost")
    String host();

    @WithDefault("8080")
    int port();

    Optional<String> contextPath();
}
