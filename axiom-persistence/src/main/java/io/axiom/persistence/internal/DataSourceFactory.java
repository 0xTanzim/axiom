package io.axiom.persistence.internal;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.axiom.persistence.config.PersistenceConfig;

/**
 * Factory for creating HikariCP DataSource instances.
 *
 * <p>
 * Internal class - use {@code AxiomPersistence} for public API.
 *
 * @since 0.1.0
 */
public final class DataSourceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DataSourceFactory.class);

    private DataSourceFactory() {}

    /**
     * Creates a HikariCP DataSource from configuration.
     *
     * @param config the persistence configuration
     * @return configured HikariDataSource
     */
    public static HikariDataSource create(PersistenceConfig config) {
        LOG.info("Creating HikariCP DataSource for: {}", maskUrl(config.url()));

        HikariConfig hikari = new HikariConfig();

        // Core settings
        hikari.setJdbcUrl(config.url());
        hikari.setUsername(config.username());
        hikari.setPassword(config.password());

        if (config.driverClassName() != null && !config.driverClassName().isBlank()) {
            hikari.setDriverClassName(config.driverClassName());
        }

        // Pool settings
        hikari.setMaximumPoolSize(config.maximumPoolSize());
        hikari.setMinimumIdle(config.minimumIdle());

        // Timeout settings
        hikari.setConnectionTimeout(config.connectionTimeout().toMillis());
        hikari.setIdleTimeout(config.idleTimeout().toMillis());
        hikari.setMaxLifetime(config.maxLifetime().toMillis());
        hikari.setValidationTimeout(config.validationTimeout().toMillis());
        hikari.setLeakDetectionThreshold(config.leakDetectionThreshold().toMillis());

        // Pool name for monitoring
        hikari.setPoolName(config.poolName());

        // Auto-commit (typically false for transaction management)
        hikari.setAutoCommit(config.autoCommit());

        // Connection test query (if provided)
        if (config.connectionTestQuery() != null && !config.connectionTestQuery().isBlank()) {
            hikari.setConnectionTestQuery(config.connectionTestQuery());
        }

        // Additional datasource properties
        config.dataSourceProperties().forEach((key, value) -> {
            hikari.addDataSourceProperty(key.toString(), value.toString());
        });

        HikariDataSource dataSource = new HikariDataSource(hikari);
        LOG.info("HikariCP DataSource created: pool={}, maxSize={}",
                config.poolName(), config.maximumPoolSize());

        return dataSource;
    }

    /**
     * Closes a DataSource if it's a HikariDataSource.
     *
     * @param dataSource the data source to close
     */
    public static void close(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource hikari) {
            String poolName = hikari.getPoolName();
            hikari.close();
            LOG.info("HikariCP DataSource closed: {}", poolName);
        }
    }

    private static String maskUrl(String url) {
        if (url == null) return "null";
        // Mask password if present in URL (jdbc:postgresql://user:pass@host/db)
        return url.replaceAll(":[^:@/]+@", ":****@");
    }
}
