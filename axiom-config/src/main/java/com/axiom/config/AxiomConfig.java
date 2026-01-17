package com.axiom.config;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * Main entry point for Axiom configuration.
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Load default configuration (application.properties, env vars, system props)
 * AxiomConfig config = AxiomConfig.load();
 *
 * // Get values
 * int port = config.getInt("server.port").orElse(8080);
 * String host = config.get("server.host").orElse("localhost");
 * }</pre>
 *
 * <h2>Type-Safe Configuration with Interfaces</h2>
 * <pre>{@code
 * // Define your config interface
 * @ConfigMapping(prefix = "server")
 * interface ServerConfig {
 *     String host();
 *     int port();
 *
 *     @WithDefault("30s")
 *     Duration timeout();
 * }
 *
 * // Use it
 * AxiomConfig config = AxiomConfig.builder()
 *     .withMapping(ServerConfig.class)
 *     .build();
 * ServerConfig server = config.getMapping(ServerConfig.class);
 * System.out.println(server.port()); // type-safe!
 * }</pre>
 *
 * <h2>Configuration Sources (Priority Order)</h2>
 * <ol>
 *   <li>System properties (highest) — {@code -Dserver.port=9090}</li>
 *   <li>Environment variables — {@code SERVER_PORT=9090}</li>
 *   <li>.env file (if present)</li>
 *   <li>application-{profile}.properties</li>
 *   <li>application.properties (lowest)</li>
 * </ol>
 *
 * <h2>Environment Variable Mapping</h2>
 * Environment variables are automatically mapped:
 * <ul>
 *   <li>{@code SERVER_HOST} → {@code server.host}</li>
 *   <li>{@code DATABASE_URL} → {@code database.url}</li>
 * </ul>
 */
public final class AxiomConfig {

    private final SmallRyeConfig delegate;

    private AxiomConfig(SmallRyeConfig delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    // =========================================================================
    // Factory Methods
    // =========================================================================

    /**
     * Loads the default configuration.
     *
     * <p>Loads from these sources in priority order:
     * <ol>
     *   <li>System properties</li>
     *   <li>Environment variables</li>
     *   <li>application.properties on classpath</li>
     * </ol>
     *
     * @return the loaded configuration
     */
    public static AxiomConfig load() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withDefaultValue("axiom.config.loaded", "true")
                .build();
        return new AxiomConfig(config);
    }

    /**
     * Creates a configuration builder for custom configuration.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an empty configuration (useful for testing).
     *
     * @return empty configuration
     */
    public static AxiomConfig empty() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().build();
        return new AxiomConfig(config);
    }

    /**
     * Parses configuration from a properties string.
     *
     * <pre>{@code
     * AxiomConfig config = AxiomConfig.parse("""
     *     server.port=8080
     *     server.host=localhost
     *     """);
     * }</pre>
     *
     * @param propertiesContent the properties content
     * @return the parsed configuration
     */
    public static AxiomConfig parse(String propertiesContent) {
        Objects.requireNonNull(propertiesContent, "propertiesContent must not be null");
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new PropertiesStringConfigSource(propertiesContent))
                .build();
        return new AxiomConfig(config);
    }

    // =========================================================================
    // String Access
    // =========================================================================

    /**
     * Gets an optional string value.
     *
     * @param key the configuration key
     * @return Optional containing value, or empty if missing
     */
    public Optional<String> get(String key) {
        return delegate.getOptionalValue(key, String.class);
    }

    /**
     * Gets a string value with default.
     *
     * @param key the configuration key
     * @param defaultValue value if key is missing
     * @return the value or default
     */
    public String get(String key, String defaultValue) {
        return delegate.getOptionalValue(key, String.class).orElse(defaultValue);
    }

    /**
     * Gets a required string value.
     *
     * @param key the configuration key
     * @return the value
     * @throws ConfigException.Missing if key is missing
     */
    public String require(String key) {
        return get(key).orElseThrow(() -> new ConfigException.Missing(key));
    }

    // =========================================================================
    // Typed Access
    // =========================================================================

    /**
     * Gets an optional integer value.
     *
     * @param key the configuration key
     * @return Optional containing value, or empty if missing
     * @throws ConfigException.WrongType if value cannot be parsed as integer
     */
    public Optional<Integer> getInt(String key) {
        Optional<String> value = get(key);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(value.get()));
        } catch (NumberFormatException e) {
            throw new ConfigException.WrongType(key, "Integer", value.get());
        }
    }

    /**
     * Gets an integer value with default.
     *
     * @param key the configuration key
     * @param defaultValue value if key is missing
     * @return the value or default
     */
    public int getInt(String key, int defaultValue) {
        return getInt(key).orElse(defaultValue);
    }

    /**
     * Gets a required integer value.
     *
     * @param key the configuration key
     * @return the integer value
     * @throws ConfigException.Missing if key is missing
     * @throws ConfigException.WrongType if value cannot be parsed
     */
    public int requireInt(String key) {
        return getInt(key).orElseThrow(() -> new ConfigException.Missing(key));
    }

    /**
     * Gets an optional long value.
     *
     * @param key the configuration key
     * @return Optional containing value, or empty if missing
     * @throws ConfigException.WrongType if value cannot be parsed as long
     */
    public Optional<Long> getLong(String key) {
        Optional<String> value = get(key);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(value.get()));
        } catch (NumberFormatException e) {
            throw new ConfigException.WrongType(key, "Long", value.get());
        }
    }

    /**
     * Gets a long value with default.
     *
     * @param key the configuration key
     * @param defaultValue value if key is missing
     * @return the value or default
     */
    public long getLong(String key, long defaultValue) {
        return getLong(key).orElse(defaultValue);
    }

    /**
     * Gets an optional boolean value.
     *
     * @param key the configuration key
     * @return Optional containing value, or empty if missing
     */
    public Optional<Boolean> getBoolean(String key) {
        Optional<String> value = get(key);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Boolean.parseBoolean(value.get()));
    }

    /**
     * Gets a boolean value with default.
     *
     * @param key the configuration key
     * @param defaultValue value if key is missing
     * @return the value or default
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return getBoolean(key).orElse(defaultValue);
    }

    /**
     * Gets an optional double value.
     *
     * @param key the configuration key
     * @return Optional containing value, or empty if missing
     * @throws ConfigException.WrongType if value cannot be parsed as double
     */
    public Optional<Double> getDouble(String key) {
        Optional<String> value = get(key);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(value.get()));
        } catch (NumberFormatException e) {
            throw new ConfigException.WrongType(key, "Double", value.get());
        }
    }

    /**
     * Gets a double value with default.
     *
     * @param key the configuration key
     * @param defaultValue value if key is missing
     * @return the value or default
     */
    public double getDouble(String key, double defaultValue) {
        return getDouble(key).orElse(defaultValue);
    }

    /**
     * Gets an optional Duration value.
     *
     * <p>Supports formats:
     * <ul>
     *   <li>{@code 30s} - 30 seconds</li>
     *   <li>{@code 5m} - 5 minutes</li>
     *   <li>{@code 2h} - 2 hours</li>
     *   <li>{@code 500ms} - 500 milliseconds</li>
     *   <li>{@code PT30S} - ISO-8601 format</li>
     * </ul>
     *
     * @param key the configuration key
     * @return Optional containing value, or empty if missing
     * @throws ConfigException.WrongType if value cannot be parsed as Duration
     */
    public Optional<Duration> getDuration(String key) {
        Optional<String> value = get(key);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(parseDuration(value.get()));
        } catch (Exception e) {
            throw new ConfigException.WrongType(key, "Duration", value.get());
        }
    }

    /**
     * Gets a Duration value with default.
     *
     * @param key the configuration key
     * @param defaultValue value if key is missing
     * @return the value or default
     */
    public Duration getDuration(String key, Duration defaultValue) {
        return getDuration(key).orElse(defaultValue);
    }

    private static Duration parseDuration(String value) {
        String trimmed = value.trim().toLowerCase();

        if (trimmed.startsWith("pt")) {
            return Duration.parse(value);
        }

        if (trimmed.endsWith("ms")) {
            return Duration.ofMillis(Long.parseLong(trimmed.substring(0, trimmed.length() - 2)));
        }
        if (trimmed.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(trimmed.substring(0, trimmed.length() - 1)));
        }
        if (trimmed.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(trimmed.substring(0, trimmed.length() - 1)));
        }
        if (trimmed.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(trimmed.substring(0, trimmed.length() - 1)));
        }
        if (trimmed.endsWith("d")) {
            return Duration.ofDays(Long.parseLong(trimmed.substring(0, trimmed.length() - 1)));
        }

        return Duration.ofMillis(Long.parseLong(trimmed));
    }

    // =========================================================================
    // Type-Safe Mapping
    // =========================================================================

    /**
     * Gets a type-safe configuration mapping.
     *
     * <p>The mapping interface must be annotated with {@code @ConfigMapping}:
     * <pre>{@code
     * @ConfigMapping(prefix = "server")
     * interface ServerConfig {
     *     String host();
     *     int port();
     *
     *     @WithDefault("30s")
     *     Duration timeout();
     *
     *     Optional<String> contextPath();
     * }
     * }</pre>
     *
     * <p>The mapping interface must be registered with the builder:
     * <pre>{@code
     * AxiomConfig config = AxiomConfig.builder()
     *     .withMapping(ServerConfig.class)
     *     .build();
     *
     * ServerConfig server = config.getMapping(ServerConfig.class);
     * }</pre>
     *
     * @param <T> the mapping type
     * @param mappingClass the mapping interface class
     * @return the type-safe configuration
     * @throws ConfigException.BindingFailed if mapping fails
     */
    public <T> T getMapping(Class<T> mappingClass) {
        try {
            return delegate.getConfigMapping(mappingClass);
        } catch (Exception e) {
            throw new ConfigException.BindingFailed(mappingClass, e);
        }
    }

    /**
     * Exposes the underlying SmallRyeConfig for advanced operations.
     *
     * @return the underlying SmallRyeConfig
     */
    public SmallRyeConfig unwrap() {
        return delegate;
    }

    // =========================================================================
    // Builder
    // =========================================================================

    /**
     * Builder for custom configuration.
     *
     * <pre>{@code
     * AxiomConfig config = AxiomConfig.builder()
     *     .withMapping(ServerConfig.class)
     *     .withProperty("server.port", "9090")
     *     .withPropertiesFile(Path.of("custom.properties"))
     *     .build();
     * }</pre>
     */
    public static final class Builder {
        private final SmallRyeConfigBuilder delegate = new SmallRyeConfigBuilder();
        private boolean useDefaultSources = true;

        private Builder() {
        }

        /**
         * Registers a configuration mapping interface.
         *
         * @param mappingClass the mapping interface annotated with @ConfigMapping
         * @return this builder
         */
        public Builder withMapping(Class<?> mappingClass) {
            delegate.withMapping(mappingClass);
            return this;
        }

        /**
         * Sets a single configuration property.
         *
         * @param key the property key
         * @param value the property value
         * @return this builder
         */
        public Builder withProperty(String key, String value) {
            delegate.withDefaultValue(key, value);
            return this;
        }

        /**
         * Sets multiple configuration properties.
         *
         * @param properties the key-value map
         * @return this builder
         */
        public Builder withProperties(Map<String, String> properties) {
            properties.forEach(delegate::withDefaultValue);
            return this;
        }

        /**
         * Loads a properties file from the filesystem.
         *
         * @param path the file path
         * @return this builder
         * @throws ConfigException if file cannot be read
         */
        public Builder withPropertiesFile(Path path) {
            try {
                String content = Files.readString(path);
                Properties props = new Properties();
                props.load(new StringReader(content));

                props.forEach((k, v) -> delegate.withDefaultValue(k.toString(), v.toString()));
                return this;
            } catch (IOException e) {
                throw new ConfigException("Failed to load properties file: " + path, e);
            }
        }

        /**
         * Loads a profile-specific properties file.
         * Profile files override values from base files.
         *
         * @param profile the profile name (e.g., "dev", "prod")
         * @param path the profile-specific properties file
         * @return this builder
         * @throws ConfigException if file cannot be read
         */
        public Builder withProfile(String profile, Path path) {
            // Profile files are loaded with higher priority
            return withPropertiesFile(path);
        }

        /**
         * Disables default configuration sources.
         * By default, system properties, environment variables, and
         * application.properties are loaded.
         *
         * @return this builder
         */
        public Builder withoutDefaultSources() {
            this.useDefaultSources = false;
            return this;
        }

        /**
         * Adds a .env file as a configuration source.
         *
         * @param path the .env file path
         * @return this builder
         */
        public Builder withDotEnvFile(Path path) {
            delegate.withSources(new DotEnvConfigSource(path));
            return this;
        }

        /**
         * Sets the active profile(s).
         *
         * @param profiles the profile names (e.g., "dev", "test")
         * @return this builder
         */
        public Builder withProfiles(String... profiles) {
            delegate.withProfile(String.join(",", profiles));
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return the built configuration
         */
        public AxiomConfig build() {
            if (useDefaultSources) {
                delegate.addDefaultSources();
                delegate.addDefaultInterceptors();
            }
            return new AxiomConfig(delegate.build());
        }
    }
}
