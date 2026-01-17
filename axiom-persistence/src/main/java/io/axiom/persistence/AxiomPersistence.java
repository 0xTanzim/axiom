package io.axiom.persistence;

import java.util.Objects;

import javax.sql.DataSource;

import org.jooq.SQLDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;

import io.axiom.persistence.config.PersistenceConfig;
import io.axiom.persistence.config.PersistenceConfigLoader;
import io.axiom.persistence.internal.DataSourceFactory;
import io.axiom.persistence.jooq.Jooq;
import io.axiom.persistence.jpa.Jpa;

/**
 * Main entry point for Axiom Persistence.
 *
 * <p>
 * This class manages the lifecycle of persistence infrastructure including
 * connection pools, JPA, and jOOQ integrations.
 *
 * <h2>Quick Start (Recommended)</h2>
 * <pre>{@code
 * // Start with auto-loaded configuration from application.properties
 * AxiomPersistence.start();
 *
 * // Use transactions - no DataSource needed!
 * Transaction.execute(() -> {
 *     Jdbc.update("INSERT INTO users (name) VALUES (?)", "John");
 * });
 *
 * // Shutdown when done
 * AxiomPersistence.stop();
 * }</pre>
 *
 * <h2>With JPA</h2>
 * <pre>{@code
 * AxiomPersistence.start(config -> config.enableJpa("my-persistence-unit"));
 *
 * Transaction.execute(() -> {
 *     Jpa.em().persist(entity);
 * });
 * }</pre>
 *
 * <h2>With jOOQ</h2>
 * <pre>{@code
 * AxiomPersistence.start(config -> config.enableJooq(SQLDialect.POSTGRES));
 *
 * Transaction.execute(() -> {
 *     Jooq.dsl().selectFrom(USERS).fetch();
 * });
 * }</pre>
 *
 * <h2>Custom Configuration</h2>
 * <pre>{@code
 * // Programmatic configuration
 * AxiomPersistence.start(PersistenceConfig.builder()
 *     .url("jdbc:postgresql://localhost/mydb")
 *     .username("user")
 *     .password("pass")
 *     .build());
 * }</pre>
 *
 * @since 0.1.0
 */
public final class AxiomPersistence {

    private static final Logger LOG = LoggerFactory.getLogger(AxiomPersistence.class);

    // Global singleton for simplified DX
    private static volatile AxiomPersistence INSTANCE;

    private final PersistenceConfig config;
    private final String jpaPersistenceUnit;
    private final SQLDialect jooqDialect;

    private volatile HikariDataSource dataSource;
    private volatile State state = State.NEW;

    private enum State { NEW, STARTING, RUNNING, STOPPING, STOPPED }

    private AxiomPersistence(Builder builder) {
        this.config = Objects.requireNonNull(builder.config, "config");
        this.jpaPersistenceUnit = builder.jpaPersistenceUnit;
        this.jooqDialect = builder.jooqDialect;
    }

    // ==================== Factory Methods ====================

    /**
     * Creates AxiomPersistence with auto-loaded configuration.
     * Configuration is loaded from application.properties.
     *
     * @return new instance
     */
    public static AxiomPersistence create() {
        return builder()
                .config(PersistenceConfigLoader.load())
                .build();
    }

    /**
     * Creates AxiomPersistence with the given configuration.
     *
     * @param config the persistence configuration
     * @return new instance
     */
    public static AxiomPersistence create(PersistenceConfig config) {
        return builder()
                .config(config)
                .build();
    }

    /**
     * Creates a builder for AxiomPersistence.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    // ==================== Global Singleton API (Simplified DX) ====================

    /**
     * Starts Axiom Persistence with auto-loaded configuration.
     *
     * <p>
     * Configuration is loaded from application.properties in the classpath.
     * After calling this method, you can use {@code Transaction.execute()}
     * without passing a DataSource.
     *
     * <pre>{@code
     * // Start once at application startup
     * AxiomPersistence.start();
     *
     * // Then use anywhere - no DataSource needed!
     * Transaction.execute(() -> {
     *     Jdbc.update("INSERT INTO users...", params);
     * });
     * }</pre>
     *
     * @throws PersistenceException if already started or startup fails
     */
    public static void start() {
        start(PersistenceConfigLoader.load());
    }

    /**
     * Starts Axiom Persistence with the given configuration.
     *
     * @param config the persistence configuration
     * @throws PersistenceException if already started or startup fails
     */
    public static void start(PersistenceConfig config) {
        start(builder().config(config));
    }

    /**
     * Starts Axiom Persistence with a builder configurator.
     *
     * <pre>{@code
     * AxiomPersistence.start(b -> b
     *     .config(myConfig)
     *     .enableJpa("my-persistence-unit")
     *     .enableJooq(SQLDialect.POSTGRES));
     * }</pre>
     *
     * @param configurator function to configure the builder
     * @throws PersistenceException if already started or startup fails
     */
    public static void start(java.util.function.Consumer<Builder> configurator) {
        Builder builder = builder().config(PersistenceConfigLoader.load());
        configurator.accept(builder);
        start(builder);
    }

    private static synchronized void start(Builder builder) {
        if (INSTANCE != null) {
            throw new PersistenceException("AxiomPersistence already started. Call stop() first.");
        }
        INSTANCE = builder.build();
        INSTANCE.startInstance();
    }

    /**
     * Stops the global Axiom Persistence instance.
     *
     * <p>
     * This shuts down all integrations and closes the connection pool.
     * After calling this, {@code Transaction.execute()} will require
     * an explicit DataSource.
     */
    public static synchronized void stop() {
        if (INSTANCE != null) {
            INSTANCE.stopInstance();
            INSTANCE = null;
        }
    }

    /**
     * Returns whether Axiom Persistence is started globally.
     *
     * @return true if started
     */
    public static boolean isStarted() {
        return INSTANCE != null && INSTANCE.isRunning();
    }

    /**
     * Returns the global DataSource.
     *
     * <p>
     * Most users don't need to call this directly - use
     * {@code Transaction.execute()} instead which automatically
     * uses this DataSource.
     *
     * @return the global data source
     * @throws PersistenceException if not started
     */
    public static DataSource globalDataSource() {
        if (INSTANCE == null) {
            throw new PersistenceException(
                "AxiomPersistence not started. Call AxiomPersistence.start() first.");
        }
        return INSTANCE.dataSource();
    }

    /**
     * Returns the global instance.
     *
     * @return the global instance
     * @throws PersistenceException if not started
     */
    public static AxiomPersistence instance() {
        if (INSTANCE == null) {
            throw new PersistenceException(
                "AxiomPersistence not started. Call AxiomPersistence.start() first.");
        }
        return INSTANCE;
    }

    // ==================== Instance Lifecycle ====================

    /**
     * Starts this persistence instance.
     *
     * <p>
     * This creates the connection pool and initializes any configured
     * integrations (JPA, jOOQ).
     *
     * <p>
     * <b>Prefer using the static {@link #start()} method</b> for simpler DX.
     *
     * @return this instance for chaining
     * @throws PersistenceException if startup fails
     */
    public synchronized AxiomPersistence startInstance() {
        if (state != State.NEW) {
            throw new PersistenceException("Cannot start: current state is " + state);
        }

        state = State.STARTING;
        LOG.info("Starting Axiom Persistence...");

        try {
            // Create connection pool
            dataSource = DataSourceFactory.create(config);

            // Initialize JPA if configured
            if (jpaPersistenceUnit != null) {
                Jpa.initialize(jpaPersistenceUnit, config);
            }

            // Initialize jOOQ if configured
            if (jooqDialect != null) {
                Jooq.initialize(dataSource, jooqDialect);
            } else if (config.url() != null) {
                // Auto-detect dialect
                SQLDialect detected = Jooq.detectDialect(config.url());
                if (detected != SQLDialect.DEFAULT) {
                    Jooq.initialize(dataSource, detected);
                }
            }

            state = State.RUNNING;
            LOG.info("Axiom Persistence started successfully");
            return this;

        } catch (Exception e) {
            state = State.STOPPED;
            cleanup();
            throw new PersistenceException("Failed to start Axiom Persistence", e);
        }
    }

    /**
     * Stops this persistence instance.
     *
     * <p>
     * This shuts down all integrations and closes the connection pool.
     *
     * <p>
     * <b>Prefer using the static {@link #stop()} method</b> for simpler DX.
     */
    public synchronized void stopInstance() {
        if (state != State.RUNNING) {
            LOG.warn("Cannot stop: current state is {}", state);
            return;
        }

        state = State.STOPPING;
        LOG.info("Stopping Axiom Persistence...");

        cleanup();

        state = State.STOPPED;
        LOG.info("Axiom Persistence stopped");
    }

    private void cleanup() {
        // Shutdown JPA
        if (Jpa.isInitialized()) {
            try {
                Jpa.shutdown();
            } catch (Exception e) {
                LOG.warn("Error shutting down JPA", e);
            }
        }

        // Shutdown jOOQ
        if (Jooq.isInitialized()) {
            try {
                Jooq.shutdown();
            } catch (Exception e) {
                LOG.warn("Error shutting down jOOQ", e);
            }
        }

        // Close connection pool
        if (dataSource != null) {
            DataSourceFactory.close(dataSource);
            dataSource = null;
        }
    }

    // ==================== Accessors ====================

    /**
     * Returns the DataSource managed by this instance.
     *
     * @return the data source
     * @throws PersistenceException if not running
     */
    public DataSource dataSource() {
        if (state != State.RUNNING) {
            throw new PersistenceException("Persistence is not running: " + state);
        }
        return dataSource;
    }

    /**
     * Returns the configuration used by this instance.
     *
     * @return the configuration
     */
    public PersistenceConfig config() {
        return config;
    }

    /**
     * Returns whether persistence is running.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return state == State.RUNNING;
    }

    /**
     * Returns the current state.
     *
     * @return current state name
     */
    public String state() {
        return state.name();
    }

    // ==================== Builder ====================

    /**
     * Builder for AxiomPersistence.
     */
    public static final class Builder {

        private PersistenceConfig config;
        private String jpaPersistenceUnit;
        private SQLDialect jooqDialect;

        Builder() {}

        /**
         * Sets the persistence configuration.
         *
         * @param config the configuration
         * @return this builder
         */
        public Builder config(PersistenceConfig config) {
            this.config = config;
            return this;
        }

        /**
         * Enables JPA with the given persistence unit.
         *
         * @param persistenceUnitName the persistence unit name from persistence.xml
         * @return this builder
         */
        public Builder enableJpa(String persistenceUnitName) {
            this.jpaPersistenceUnit = persistenceUnitName;
            return this;
        }

        /**
         * Enables jOOQ with the given SQL dialect.
         *
         * @param dialect the SQL dialect
         * @return this builder
         */
        public Builder enableJooq(SQLDialect dialect) {
            this.jooqDialect = dialect;
            return this;
        }

        /**
         * Enables jOOQ with auto-detected dialect.
         *
         * @return this builder
         */
        public Builder enableJooq() {
            this.jooqDialect = SQLDialect.DEFAULT;
            return this;
        }

        /**
         * Builds the AxiomPersistence instance.
         *
         * @return new instance
         */
        public AxiomPersistence build() {
            return new AxiomPersistence(this);
        }
    }
}
