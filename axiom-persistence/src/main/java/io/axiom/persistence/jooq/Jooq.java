package io.axiom.persistence.jooq;

import java.sql.Connection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.axiom.persistence.tx.TransactionContext;
import io.axiom.persistence.tx.TransactionException;

/**
 * jOOQ integration providing DSLContext access within Axiom transactions.
 *
 * <p>
 * This class provides transaction-aware jOOQ DSLContext instances that
 * automatically use the current transaction's connection.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Initialize jOOQ
 * Jooq.initialize(dataSource, SQLDialect.POSTGRES);
 *
 * // Use within a transaction
 * Transaction.execute(dataSource, () -> {
 *     DSLContext dsl = Jooq.dsl();
 *
 *     // Type-safe query with generated code
 *     List<UserRecord> users = dsl
 *         .selectFrom(USERS)
 *         .where(USERS.ACTIVE.isTrue())
 *         .fetch();
 *
 *     // Plain SQL
 *     dsl.execute("UPDATE users SET last_login = NOW() WHERE id = ?", userId);
 * });
 * }</pre>
 *
 * <h2>Configuration</h2>
 * <pre>{@code
 * // Custom settings
 * Settings settings = new Settings()
 *     .withRenderFormatted(true)
 *     .withExecuteLogging(true);
 *
 * Jooq.initialize(dataSource, SQLDialect.POSTGRES, settings);
 * }</pre>
 *
 * @since 0.1.0
 */
public final class Jooq {

    private static final Logger LOG = LoggerFactory.getLogger(Jooq.class);

    private static final String DEFAULT_KEY = "default";
    private static final Map<String, Configuration> CONFIGURATIONS = new ConcurrentHashMap<>();
    private static final Map<String, DataSource> DATA_SOURCES = new ConcurrentHashMap<>();

    private Jooq() {}

    // ==================== Initialization ====================

    /**
     * Initializes jOOQ with a data source and SQL dialect.
     *
     * @param dataSource the data source
     * @param dialect the SQL dialect
     */
    public static void initialize(DataSource dataSource, SQLDialect dialect) {
        initialize(DEFAULT_KEY, dataSource, dialect, new Settings());
    }

    /**
     * Initializes jOOQ with custom settings.
     *
     * @param dataSource the data source
     * @param dialect the SQL dialect
     * @param settings jOOQ settings
     */
    public static void initialize(DataSource dataSource, SQLDialect dialect, Settings settings) {
        initialize(DEFAULT_KEY, dataSource, dialect, settings);
    }

    /**
     * Initializes a named jOOQ configuration.
     *
     * @param name configuration name
     * @param dataSource the data source
     * @param dialect the SQL dialect
     * @param settings jOOQ settings
     */
    public static void initialize(String name, DataSource dataSource,
                                  SQLDialect dialect, Settings settings) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(dataSource, "dataSource");
        Objects.requireNonNull(dialect, "dialect");
        Objects.requireNonNull(settings, "settings");

        if (CONFIGURATIONS.containsKey(name)) {
            throw new JooqException("jOOQ configuration already initialized: " + name);
        }

        Configuration config = new DefaultConfiguration()
                .set(dataSource)
                .set(dialect)
                .set(settings);

        CONFIGURATIONS.put(name, config);
        DATA_SOURCES.put(name, dataSource);

        LOG.info("jOOQ initialized: name={}, dialect={}", name, dialect);
    }

    // ==================== DSLContext Access ====================

    /**
     * Returns a DSLContext using the current transaction's connection.
     *
     * @return the DSLContext
     * @throws TransactionException if no active transaction
     * @throws JooqException if jOOQ is not initialized
     */
    public static DSLContext dsl() {
        return dsl(DEFAULT_KEY);
    }

    /**
     * Returns a DSLContext for a named configuration.
     *
     * @param name configuration name
     * @return the DSLContext
     */
    public static DSLContext dsl(String name) {
        Configuration config = CONFIGURATIONS.get(name);
        if (config == null) {
            throw new JooqException("jOOQ not initialized: " + name + ". Call Jooq.initialize() first.");
        }

        // Get connection from current transaction
        Connection connection = TransactionContext.current()
                .map(TransactionContext::connection)
                .orElseThrow(() -> new TransactionException(
                    "Jooq.dsl() requires an active transaction. Use Transaction.execute() first."
                ));

        // Create DSLContext with transaction connection
        return DSL.using(connection, config.dialect(), config.settings());
    }

    /**
     * Returns a standalone DSLContext (not bound to a transaction).
     * Use this for operations that manage their own connections.
     *
     * @return the DSLContext
     */
    public static DSLContext dslStandalone() {
        return dslStandalone(DEFAULT_KEY);
    }

    /**
     * Returns a standalone DSLContext for a named configuration.
     *
     * @param name configuration name
     * @return the DSLContext
     */
    public static DSLContext dslStandalone(String name) {
        Configuration config = CONFIGURATIONS.get(name);
        if (config == null) {
            throw new JooqException("jOOQ not initialized: " + name);
        }
        return DSL.using(config);
    }

    /**
     * Returns a DSLContext with a specific connection.
     *
     * @param connection the JDBC connection to use
     * @return the DSLContext
     */
    public static DSLContext dsl(Connection connection) {
        return dsl(DEFAULT_KEY, connection);
    }

    /**
     * Returns a DSLContext with a specific connection.
     *
     * @param name configuration name
     * @param connection the JDBC connection to use
     * @return the DSLContext
     */
    public static DSLContext dsl(String name, Connection connection) {
        Configuration config = CONFIGURATIONS.get(name);
        if (config == null) {
            throw new JooqException("jOOQ not initialized: " + name);
        }
        return DSL.using(connection, config.dialect(), config.settings());
    }

    // ==================== Configuration Access ====================

    /**
     * Returns the jOOQ Configuration.
     *
     * @return the Configuration
     */
    public static Configuration configuration() {
        return configuration(DEFAULT_KEY);
    }

    /**
     * Returns a named jOOQ Configuration.
     *
     * @param name configuration name
     * @return the Configuration
     */
    public static Configuration configuration(String name) {
        Configuration config = CONFIGURATIONS.get(name);
        if (config == null) {
            throw new JooqException("jOOQ not initialized: " + name);
        }
        return config;
    }

    /**
     * Checks if jOOQ is initialized.
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return !CONFIGURATIONS.isEmpty();
    }

    /**
     * Checks if a named configuration is initialized.
     *
     * @param name configuration name
     * @return true if initialized
     */
    public static boolean isInitialized(String name) {
        return CONFIGURATIONS.containsKey(name);
    }

    // ==================== Shutdown ====================

    /**
     * Removes all jOOQ configurations.
     */
    public static void shutdown() {
        CONFIGURATIONS.clear();
        DATA_SOURCES.clear();
        LOG.info("jOOQ shutdown complete");
    }

    /**
     * Removes a named configuration.
     *
     * @param name configuration name
     */
    public static void shutdown(String name) {
        CONFIGURATIONS.remove(name);
        DATA_SOURCES.remove(name);
        LOG.info("jOOQ shutdown: {}", name);
    }

    // ==================== Utility Methods ====================

    /**
     * Detects SQL dialect from a JDBC URL.
     *
     * @param jdbcUrl the JDBC URL
     * @return detected dialect
     */
    public static SQLDialect detectDialect(String jdbcUrl) {
        if (jdbcUrl == null) {
            return SQLDialect.DEFAULT;
        }

        String lower = jdbcUrl.toLowerCase();
        if (lower.contains(":postgresql:") || lower.contains(":pgsql:")) {
            return SQLDialect.POSTGRES;
        }
        if (lower.contains(":mysql:")) {
            return SQLDialect.MYSQL;
        }
        if (lower.contains(":mariadb:")) {
            return SQLDialect.MARIADB;
        }
        if (lower.contains(":h2:")) {
            return SQLDialect.H2;
        }
        if (lower.contains(":hsqldb:")) {
            return SQLDialect.HSQLDB;
        }
        if (lower.contains(":derby:")) {
            return SQLDialect.DERBY;
        }
        if (lower.contains(":sqlite:")) {
            return SQLDialect.SQLITE;
        }
        if (lower.contains(":oracle:")) {
            return SQLDialect.DEFAULT; // Oracle requires commercial license
        }
        if (lower.contains(":sqlserver:") || lower.contains(":microsoft:")) {
            return SQLDialect.DEFAULT; // SQL Server requires commercial license
        }

        LOG.warn("Could not detect SQL dialect from URL: {}, using DEFAULT", jdbcUrl);
        return SQLDialect.DEFAULT;
    }
}
