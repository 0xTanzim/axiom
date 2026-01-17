package io.axiom.persistence.config;

import java.time.Duration;
import java.util.*;

/**
 * Immutable configuration for Axiom Persistence.
 *
 * <p>
 * Supports both properties file and programmatic configuration.
 * Uses builder pattern with sensible defaults aligned with HikariCP.
 *
 * <h2>Properties File</h2>
 * <pre>
 * axiom.datasource.url=jdbc:postgresql://localhost/mydb
 * axiom.datasource.username=user
 * axiom.datasource.password=secret
 * axiom.datasource.pool.maximum-size=10
 * </pre>
 *
 * <h2>Programmatic</h2>
 * <pre>{@code
 * PersistenceConfig config = PersistenceConfig.builder()
 *     .url("jdbc:postgresql://localhost/mydb")
 *     .username("user")
 *     .password("secret")
 *     .build();
 * }</pre>
 *
 * @since 0.1.0
 */
public record PersistenceConfig(
        String url,
        String username,
        String password,
        String driverClassName,
        int maximumPoolSize,
        int minimumIdle,
        Duration connectionTimeout,
        Duration idleTimeout,
        Duration maxLifetime,
        Duration validationTimeout,
        Duration leakDetectionThreshold,
        boolean autoCommit,
        String poolName,
        String connectionTestQuery,
        Map<Object, Object> hibernateProperties,
        Map<Object, Object> dataSourceProperties
) {

    // Property keys
    public static final String PROP_URL = "axiom.datasource.url";
    public static final String PROP_USERNAME = "axiom.datasource.username";
    public static final String PROP_PASSWORD = "axiom.datasource.password";
    public static final String PROP_DRIVER = "axiom.datasource.driver-class-name";
    public static final String PROP_POOL_SIZE = "axiom.datasource.pool.maximum-size";
    public static final String PROP_MIN_IDLE = "axiom.datasource.pool.minimum-idle";
    public static final String PROP_CONNECTION_TIMEOUT = "axiom.datasource.pool.connection-timeout";
    public static final String PROP_IDLE_TIMEOUT = "axiom.datasource.pool.idle-timeout";
    public static final String PROP_MAX_LIFETIME = "axiom.datasource.pool.max-lifetime";
    public static final String PROP_VALIDATION_TIMEOUT = "axiom.datasource.pool.validation-timeout";
    public static final String PROP_LEAK_DETECTION = "axiom.datasource.pool.leak-detection-threshold";
    public static final String PROP_AUTO_COMMIT = "axiom.datasource.auto-commit";
    public static final String PROP_POOL_NAME = "axiom.datasource.pool.name";
    public static final String PROP_TEST_QUERY = "axiom.datasource.connection-test-query";

    // Defaults (aligned with HikariCP)
    public static final int DEFAULT_MAXIMUM_POOL_SIZE = 10;
    public static final int DEFAULT_MINIMUM_IDLE = 10;
    public static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(30);
    public static final Duration DEFAULT_IDLE_TIMEOUT = Duration.ofMinutes(10);
    public static final Duration DEFAULT_MAX_LIFETIME = Duration.ofMinutes(30);
    public static final Duration DEFAULT_VALIDATION_TIMEOUT = Duration.ofSeconds(5);
    public static final Duration DEFAULT_LEAK_DETECTION = Duration.ZERO;
    public static final boolean DEFAULT_AUTO_COMMIT = false;
    public static final String DEFAULT_POOL_NAME = "axiom-pool";

    /**
     * Canonical constructor with validation.
     */
    public PersistenceConfig {
        Objects.requireNonNull(connectionTimeout, "connectionTimeout");
        Objects.requireNonNull(idleTimeout, "idleTimeout");
        Objects.requireNonNull(maxLifetime, "maxLifetime");
        Objects.requireNonNull(validationTimeout, "validationTimeout");
        Objects.requireNonNull(leakDetectionThreshold, "leakDetectionThreshold");
        Objects.requireNonNull(poolName, "poolName");

        if (maximumPoolSize < 1) {
            throw new IllegalArgumentException("maximumPoolSize must be >= 1, got: " + maximumPoolSize);
        }
        if (minimumIdle < 0) {
            throw new IllegalArgumentException("minimumIdle cannot be negative, got: " + minimumIdle);
        }
        if (minimumIdle > maximumPoolSize) {
            minimumIdle = maximumPoolSize;
        }

        hibernateProperties = hibernateProperties != null
                ? Collections.unmodifiableMap(new HashMap<>(hibernateProperties))
                : Map.of();
        dataSourceProperties = dataSourceProperties != null
                ? Collections.unmodifiableMap(new HashMap<>(dataSourceProperties))
                : Map.of();
    }

    /**
     * Creates a new builder with default values.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates configuration from properties.
     *
     * @param props the properties
     * @return configuration
     */
    public static PersistenceConfig fromProperties(Properties props) {
        Builder builder = new Builder()
                .url(props.getProperty(PROP_URL))
                .username(props.getProperty(PROP_USERNAME))
                .password(props.getProperty(PROP_PASSWORD))
                .driverClassName(props.getProperty(PROP_DRIVER))
                .maximumPoolSize(parseIntOr(props, PROP_POOL_SIZE, DEFAULT_MAXIMUM_POOL_SIZE))
                .minimumIdle(parseIntOr(props, PROP_MIN_IDLE, DEFAULT_MINIMUM_IDLE))
                .connectionTimeout(parseDurationMillisOr(props, PROP_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT))
                .idleTimeout(parseDurationMillisOr(props, PROP_IDLE_TIMEOUT, DEFAULT_IDLE_TIMEOUT))
                .maxLifetime(parseDurationMillisOr(props, PROP_MAX_LIFETIME, DEFAULT_MAX_LIFETIME))
                .validationTimeout(parseDurationMillisOr(props, PROP_VALIDATION_TIMEOUT, DEFAULT_VALIDATION_TIMEOUT))
                .leakDetectionThreshold(parseDurationMillisOr(props, PROP_LEAK_DETECTION, DEFAULT_LEAK_DETECTION))
                .autoCommit(parseBooleanOr(props, PROP_AUTO_COMMIT, DEFAULT_AUTO_COMMIT))
                .poolName(props.getProperty(PROP_POOL_NAME, DEFAULT_POOL_NAME))
                .connectionTestQuery(props.getProperty(PROP_TEST_QUERY));

        // Extract hibernate.* properties
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("hibernate.")) {
                builder.hibernateProperty(key, props.getProperty(key));
            }
        }

        return builder.build();
    }

    private static int parseIntOr(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Duration parseDurationMillisOr(Properties props, String key, Duration defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            long millis = Long.parseLong(value.trim());
            return Duration.ofMillis(millis);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean parseBooleanOr(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * Builder for PersistenceConfig.
     */
    public static final class Builder {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
        private int maximumPoolSize = DEFAULT_MAXIMUM_POOL_SIZE;
        private int minimumIdle = DEFAULT_MINIMUM_IDLE;
        private Duration connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        private Duration idleTimeout = DEFAULT_IDLE_TIMEOUT;
        private Duration maxLifetime = DEFAULT_MAX_LIFETIME;
        private Duration validationTimeout = DEFAULT_VALIDATION_TIMEOUT;
        private Duration leakDetectionThreshold = DEFAULT_LEAK_DETECTION;
        private boolean autoCommit = DEFAULT_AUTO_COMMIT;
        private String poolName = DEFAULT_POOL_NAME;
        private String connectionTestQuery;
        private Map<Object, Object> hibernateProperties = new HashMap<>();
        private Map<Object, Object> dataSourceProperties = new HashMap<>();

        private Builder() {}

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder driverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
            return this;
        }

        public Builder maximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        public Builder minimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
            return this;
        }

        public Builder connectionTimeout(Duration connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder idleTimeout(Duration idleTimeout) {
            this.idleTimeout = idleTimeout;
            return this;
        }

        public Builder maxLifetime(Duration maxLifetime) {
            this.maxLifetime = maxLifetime;
            return this;
        }

        public Builder validationTimeout(Duration validationTimeout) {
            this.validationTimeout = validationTimeout;
            return this;
        }

        public Builder leakDetectionThreshold(Duration leakDetectionThreshold) {
            this.leakDetectionThreshold = leakDetectionThreshold;
            return this;
        }

        public Builder autoCommit(boolean autoCommit) {
            this.autoCommit = autoCommit;
            return this;
        }

        public Builder poolName(String poolName) {
            this.poolName = poolName;
            return this;
        }

        public Builder connectionTestQuery(String connectionTestQuery) {
            this.connectionTestQuery = connectionTestQuery;
            return this;
        }

        public Builder hibernateProperty(String key, String value) {
            this.hibernateProperties.put(key, value);
            return this;
        }

        public Builder hibernateProperties(Map<?, ?> properties) {
            this.hibernateProperties.putAll(properties);
            return this;
        }

        public Builder dataSourceProperty(String key, Object value) {
            this.dataSourceProperties.put(key, value);
            return this;
        }

        public Builder dataSourceProperties(Map<?, ?> properties) {
            this.dataSourceProperties.putAll(properties);
            return this;
        }

        public PersistenceConfig build() {
            return new PersistenceConfig(
                    url,
                    username,
                    password,
                    driverClassName,
                    maximumPoolSize,
                    minimumIdle,
                    connectionTimeout,
                    idleTimeout,
                    maxLifetime,
                    validationTimeout,
                    leakDetectionThreshold,
                    autoCommit,
                    poolName,
                    connectionTestQuery,
                    hibernateProperties,
                    dataSourceProperties
            );
        }
    }
}
