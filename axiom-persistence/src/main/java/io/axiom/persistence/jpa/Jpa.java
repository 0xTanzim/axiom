package io.axiom.persistence.jpa;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.axiom.persistence.config.PersistenceConfig;
import io.axiom.persistence.tx.TransactionContext;
import io.axiom.persistence.tx.TransactionException;

/**
 * JPA integration providing EntityManager access within Axiom transactions.
 *
 * <p>
 * This class manages EntityManagerFactory lifecycle and provides
 * transaction-scoped EntityManager instances.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Initialize JPA (typically at startup)
 * Jpa.initialize("my-persistence-unit", config);
 *
 * // Use within a transaction
 * Transaction.execute(dataSource, () -> {
 *     EntityManager em = Jpa.em();
 *     em.persist(entity);
 *     em.flush();
 * });
 *
 * // Shutdown
 * Jpa.shutdown();
 * }</pre>
 *
 * @since 0.1.0
 */
public final class Jpa {

    private static final Logger LOG = LoggerFactory.getLogger(Jpa.class);

    private static final String DEFAULT_UNIT = "default";
    private static final Map<String, EntityManagerFactory> FACTORIES = new ConcurrentHashMap<>();

    /**
     * Scoped value for transaction-bound EntityManager.
     */
    private static final ScopedValue<EntityManager> CURRENT_EM = ScopedValue.newInstance();

    private Jpa() {}

    // ==================== Initialization ====================

    /**
     * Initializes JPA with the given persistence unit name.
     *
     * @param persistenceUnitName the persistence unit name from persistence.xml
     * @return the created EntityManagerFactory
     */
    public static EntityManagerFactory initialize(String persistenceUnitName) {
        return initialize(persistenceUnitName, Map.of());
    }

    /**
     * Initializes JPA with properties.
     *
     * @param persistenceUnitName the persistence unit name
     * @param properties additional properties to pass to JPA provider
     * @return the created EntityManagerFactory
     */
    public static EntityManagerFactory initialize(String persistenceUnitName,
                                                  Map<String, Object> properties) {
        Objects.requireNonNull(persistenceUnitName, "persistenceUnitName");

        if (FACTORIES.containsKey(persistenceUnitName)) {
            throw new JpaException("Persistence unit already initialized: " + persistenceUnitName);
        }

        LOG.info("Initializing JPA persistence unit: {}", persistenceUnitName);
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnitName, properties);
        FACTORIES.put(persistenceUnitName, emf);

        if (FACTORIES.size() == 1) {
            FACTORIES.put(DEFAULT_UNIT, emf);
        }

        LOG.info("JPA initialized for persistence unit: {}", persistenceUnitName);
        return emf;
    }

    /**
     * Initializes JPA from PersistenceConfig.
     * Uses Hibernate-specific properties from the config.
     *
     * @param persistenceUnitName the persistence unit name
     * @param config the persistence configuration
     * @return the created EntityManagerFactory
     */
    public static EntityManagerFactory initialize(String persistenceUnitName, PersistenceConfig config) {
        Map<String, Object> props = new java.util.HashMap<>();

        // JDBC properties
        props.put("jakarta.persistence.jdbc.url", config.url());
        props.put("jakarta.persistence.jdbc.user", config.username());
        props.put("jakarta.persistence.jdbc.password", config.password());
        if (config.driverClassName() != null) {
            props.put("jakarta.persistence.jdbc.driver", config.driverClassName());
        }

        // Add Hibernate properties from config
        config.hibernateProperties().forEach((k, v) -> props.put(k.toString(), v));

        return initialize(persistenceUnitName, props);
    }

    // ==================== EntityManager Access ====================

    /**
     * Returns the EntityManager for the current transaction.
     *
     * <p>
     * The EntityManager is bound to the current transaction and will be
     * closed when the transaction completes.
     *
     * @return the EntityManager for current transaction
     * @throws TransactionException if no active transaction
     * @throws JpaException if JPA is not initialized
     */
    public static EntityManager em() {
        return em(DEFAULT_UNIT);
    }

    /**
     * Returns the EntityManager for a specific persistence unit.
     *
     * @param persistenceUnitName the persistence unit name
     * @return the EntityManager
     */
    public static EntityManager em(String persistenceUnitName) {
        // Check if we have a transaction-bound EntityManager
        if (CURRENT_EM.isBound()) {
            return CURRENT_EM.get();
        }

        // Require an active transaction
        TransactionContext ctx = TransactionContext.current()
                .orElseThrow(() -> new TransactionException(
                    "Jpa.em() requires an active transaction. Use Transaction.execute() first."
                ));

        EntityManagerFactory emf = FACTORIES.get(persistenceUnitName);
        if (emf == null) {
            throw new JpaException("Persistence unit not initialized: " + persistenceUnitName +
                ". Call Jpa.initialize() first.");
        }

        // Create EntityManager joined to the current transaction
        EntityManager em = emf.createEntityManager();
        em.joinTransaction();

        return em;
    }

    /**
     * Returns the EntityManagerFactory for the default persistence unit.
     *
     * @return the EntityManagerFactory
     * @throws JpaException if not initialized
     */
    public static EntityManagerFactory emf() {
        return emf(DEFAULT_UNIT);
    }

    /**
     * Returns the EntityManagerFactory for a specific persistence unit.
     *
     * @param persistenceUnitName the persistence unit name
     * @return the EntityManagerFactory
     * @throws JpaException if not initialized
     */
    public static EntityManagerFactory emf(String persistenceUnitName) {
        EntityManagerFactory emf = FACTORIES.get(persistenceUnitName);
        if (emf == null) {
            throw new JpaException("Persistence unit not initialized: " + persistenceUnitName);
        }
        return emf;
    }

    /**
     * Checks if JPA is initialized.
     *
     * @return true if at least one persistence unit is initialized
     */
    public static boolean isInitialized() {
        return !FACTORIES.isEmpty();
    }

    /**
     * Checks if a specific persistence unit is initialized.
     *
     * @param persistenceUnitName the persistence unit name
     * @return true if initialized
     */
    public static boolean isInitialized(String persistenceUnitName) {
        return FACTORIES.containsKey(persistenceUnitName);
    }

    // ==================== Shutdown ====================

    /**
     * Shuts down all EntityManagerFactories.
     */
    public static void shutdown() {
        FACTORIES.entrySet().removeIf(entry -> {
            if (!DEFAULT_UNIT.equals(entry.getKey())) {
                closeFactory(entry.getKey(), entry.getValue());
            }
            return true;
        });
        LOG.info("JPA shutdown complete");
    }

    /**
     * Shuts down a specific persistence unit.
     *
     * @param persistenceUnitName the persistence unit to close
     */
    public static void shutdown(String persistenceUnitName) {
        EntityManagerFactory emf = FACTORIES.remove(persistenceUnitName);
        if (emf != null) {
            closeFactory(persistenceUnitName, emf);
        }
    }

    private static void closeFactory(String name, EntityManagerFactory emf) {
        try {
            if (emf.isOpen()) {
                emf.close();
                LOG.info("Closed EntityManagerFactory: {}", name);
            }
        } catch (Exception e) {
            LOG.warn("Error closing EntityManagerFactory {}: {}", name, e.getMessage());
        }
    }

    // ==================== Scoped Value Support ====================

    /**
     * Returns the scoped value for EntityManager binding.
     * Used internally for transaction-scoped EntityManager management.
     */
    static ScopedValue<EntityManager> scopedValue() {
        return CURRENT_EM;
    }
}
