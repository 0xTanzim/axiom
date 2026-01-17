/**
 * Axiom Persistence Module.
 *
 * <p>
 * Provides unified persistence infrastructure for Axiom applications including:
 * <ul>
 *   <li>Connection pooling via HikariCP</li>
 *   <li>Transaction management with Scoped Values</li>
 *   <li>JPA/Hibernate integration</li>
 *   <li>jOOQ type-safe SQL integration</li>
 *   <li>Simple JDBC helpers</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Start persistence
 * var persistence = AxiomPersistence.create();
 * persistence.start();
 *
 * // Use transactions
 * Transaction.execute(persistence.dataSource(), () -> {
 *     Jdbc.update("INSERT INTO users (name) VALUES (?)", "John");
 * });
 *
 * // Shutdown
 * persistence.stop();
 * }</pre>
 *
 * @since 0.1.0
 */
module io.axiom.persistence {
    // Core dependencies
    requires transitive java.sql;
    requires transitive io.axiom.core;

    // HikariCP for connection pooling
    requires com.zaxxer.hikari;

    // Logging
    requires org.slf4j;

    // JPA (optional at runtime)
    requires static jakarta.persistence;
    requires static org.hibernate.orm.core;

    // jOOQ (optional at runtime)
    requires static org.jooq;

    // Exported packages
    exports io.axiom.persistence;
    exports io.axiom.persistence.config;
    exports io.axiom.persistence.tx;
    exports io.axiom.persistence.jdbc;
    exports io.axiom.persistence.jpa;
    exports io.axiom.persistence.jooq;
}
