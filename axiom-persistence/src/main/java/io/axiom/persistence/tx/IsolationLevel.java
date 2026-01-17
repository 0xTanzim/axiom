package io.axiom.persistence.tx;

import java.sql.Connection;

/**
 * Transaction isolation levels mapping to JDBC isolation levels.
 *
 * @since 0.1.0
 */
public enum IsolationLevel {

    /**
     * Use the default isolation level of the underlying database.
     */
    DEFAULT(-1),

    /**
     * Dirty reads, non-repeatable reads and phantom reads can occur.
     */
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),

    /**
     * Dirty reads are prevented; non-repeatable reads and phantom reads can occur.
     */
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),

    /**
     * Dirty reads and non-repeatable reads are prevented; phantom reads can occur.
     */
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),

    /**
     * Dirty reads, non-repeatable reads and phantom reads are prevented.
     */
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);

    private final int jdbcLevel;

    IsolationLevel(int jdbcLevel) {
        this.jdbcLevel = jdbcLevel;
    }

    /**
     * Returns the JDBC isolation level constant.
     *
     * @return JDBC isolation level
     */
    public int jdbcLevel() {
        return jdbcLevel;
    }

    /**
     * Returns true if this is the DEFAULT level (use database default).
     *
     * @return true if default
     */
    public boolean isDefault() {
        return this == DEFAULT;
    }

    /**
     * Converts a JDBC isolation level constant to IsolationLevel.
     *
     * @param jdbcLevel the JDBC constant
     * @return corresponding IsolationLevel
     * @throws IllegalArgumentException if unknown level
     */
    public static IsolationLevel fromJdbc(int jdbcLevel) {
        return switch (jdbcLevel) {
            case Connection.TRANSACTION_READ_UNCOMMITTED -> READ_UNCOMMITTED;
            case Connection.TRANSACTION_READ_COMMITTED -> READ_COMMITTED;
            case Connection.TRANSACTION_REPEATABLE_READ -> REPEATABLE_READ;
            case Connection.TRANSACTION_SERIALIZABLE -> SERIALIZABLE;
            default -> throw new IllegalArgumentException("Unknown JDBC isolation level: " + jdbcLevel);
        };
    }
}
