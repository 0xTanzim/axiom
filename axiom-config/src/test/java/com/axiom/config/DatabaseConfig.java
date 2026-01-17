package com.axiom.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Test configuration mapping for database settings with nested pool config.
 */
@ConfigMapping(prefix = "database")
public interface DatabaseConfig {

    @WithDefault("localhost")
    String host();

    @WithDefault("5432")
    int port();

    PoolConfig pool();

    interface PoolConfig {
        @WithName("min-size")
        @WithDefault("5")
        int minSize();

        @WithName("max-size")
        @WithDefault("10")
        int maxSize();
    }
}
