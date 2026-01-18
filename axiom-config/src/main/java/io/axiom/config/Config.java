package io.axiom.config;

import java.time.*;
import java.util.*;

/**
 * Simple, static configuration access for Axiom applications.
 *
 * <h2>Getting Values</h2>
 * <pre>{@code
 * // String values
 * String dbUrl = Config.get("database.url");
 * String host = Config.get("server.host", "localhost");
 *
 * // Typed values with defaults
 * int port = Config.get("server.port", 8080);
 * boolean debug = Config.get("app.debug", false);
 * Duration timeout = Config.get("server.timeout", Duration.ofSeconds(30));
 * }</pre>
 *
 * <h2>Binding to Records</h2>
 * <pre>{@code
 * record DatabaseConfig(String url, String username, String password) {}
 *
 * DatabaseConfig db = Config.bind("database", DatabaseConfig.class);
 * // Reads: database.url, database.username, database.password
 * }</pre>
 *
 * <h2>Configuration Sources (Automatic)</h2>
 * <p>Config is loaded automatically from (highest priority first):
 * <ol>
 *   <li>System properties (-Dkey=value)</li>
 *   <li>Environment variables (KEY_NAME → key.name)</li>
 *   <li>.env file (if present)</li>
 *   <li>application-{profile}.properties</li>
 *   <li>application.properties</li>
 * </ol>
 *
 * <p>You don't need to configure this. It just works.
 *
 * @since 0.1.0
 */
public final class Config {

    private static volatile AxiomConfig instance;
    private static final Object LOCK = new Object();

    private Config() {}

    // =========================================================================
    // String Values
    // =========================================================================

    /**
     * Gets a required configuration value.
     *
     * @param key the configuration key (e.g., "database.url")
     * @return the value
     * @throws ConfigException.Missing if key is not found
     */
    public static String get(final String key) {
        return Config.config().require(key);
    }

    /**
     * Gets a configuration value with default.
     *
     * @param key the configuration key
     * @param defaultValue value if key is missing
     * @return the value or default
     */
    public static String get(final String key, final String defaultValue) {
        return Config.config().get(key, defaultValue);
    }

    // =========================================================================
    // Typed Values
    // =========================================================================

    /**
     * Gets an integer configuration value with default.
     *
     * @param key the configuration key
     * @param defaultValue value if key is missing
     * @return the value or default
     */
    public static int get(final String key, final int defaultValue) {
        return Config.config().getInt(key, defaultValue);
    }

    /**
     * Gets a long configuration value with default.
     *
     * @param key the configuration key
     * @param defaultValue value if key is missing
     * @return the value or default
     */
    public static long get(final String key, final long defaultValue) {
        return Config.config().getLong(key, defaultValue);
    }

    /**
     * Gets a boolean configuration value with default.
     *
     * @param key the configuration key
     * @param defaultValue value if key is missing
     * @return the value or default
     */
    public static boolean get(final String key, final boolean defaultValue) {
        return Config.config().getBoolean(key, defaultValue);
    }

    /**
     * Gets a Duration configuration value with default.
     *
     * <p>Supports formats: 30s, 5m, 2h, 500ms, PT30S
     *
     * @param key the configuration key
     * @param defaultValue value if key is missing
     * @return the value or default
     */
    public static Duration get(final String key, final Duration defaultValue) {
        return Config.config().getDuration(key, defaultValue);
    }

    /**
     * Gets an optional configuration value.
     *
     * @param key the configuration key
     * @return Optional containing value, or empty if missing
     */
    public static Optional<String> getOptional(final String key) {
        return Config.config().get(key);
    }

    // =========================================================================
    // Record Binding
    // =========================================================================

    /**
     * Binds configuration to a record type.
     *
     * <p>Example:
     * <pre>{@code
     * record ServerConfig(String host, int port) {}
     *
     * // Reads server.host and server.port
     * ServerConfig server = Config.bind("server", ServerConfig.class);
     * }</pre>
     *
     * @param <T> the record type
     * @param prefix the configuration prefix (e.g., "server", "database")
     * @param recordType the record class
     * @return a new record instance with bound values
     * @throws ConfigException if binding fails
     */
    public static <T extends Record> T bind(final String prefix, final Class<T> recordType) {
        return RecordBinder.bind(Config.config(), prefix, recordType);
    }

    // =========================================================================
    // Framework Configs (Pre-built)
    // =========================================================================

    /**
     * Gets the server configuration.
     *
     * <p>Reads from: server.host, server.port, server.contextPath
     *
     * @return server configuration with defaults
     */
    public static ServerConfig server() {
        return new ServerConfig(
            Config.get("server.host", "0.0.0.0"),
            Config.get("server.port", 8080),
            Config.get("server.contextPath", "/")
        );
    }

    /**
     * Gets the database configuration.
     *
     * <p>Reads from: database.url, database.username, database.password, database.poolSize
     *
     * @return database configuration (url is required, others have defaults)
     */
    public static DatabaseConfig database() {
        return new DatabaseConfig(
            Config.get("database.url"),
            Config.get("database.username", ""),
            Config.get("database.password", ""),
            Config.get("database.poolSize", 10)
        );
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Checks if configuration has been loaded.
     *
     * @return true if config is ready
     */
    public static boolean isLoaded() {
        return Config.instance != null;
    }

    /**
     * Gets the underlying AxiomConfig instance.
     * For internal use or advanced scenarios.
     *
     * @return the AxiomConfig instance
     */
    public static AxiomConfig raw() {
        return Config.config();
    }

    // =========================================================================
    // Internal
    // =========================================================================

    private static AxiomConfig config() {
        if (Config.instance == null) {
            synchronized (Config.LOCK) {
                if (Config.instance == null) {
                    Config.instance = AxiomConfig.load();
                }
            }
        }
        return Config.instance;
    }

    /**
     * Initializes config with a custom instance.
     * For testing or programmatic configuration.
     *
     * @param config the config instance to use
     */
    public static void init(final AxiomConfig config) {
        synchronized (Config.LOCK) {
            Config.instance = config;
        }
    }

    /**
     * Resets the configuration (for testing).
     */
    static void reset() {
        synchronized (Config.LOCK) {
            Config.instance = null;
        }
    }

    // =========================================================================
    // Pre-built Config Records
    // =========================================================================

    /**
     * Server configuration.
     */
    public record ServerConfig(
        String host,
        int port,
        String contextPath
    ) {}

    /**
     * Database configuration.
     */
    public record DatabaseConfig(
        String url,
        String username,
        String password,
        int poolSize
    ) {}
}
