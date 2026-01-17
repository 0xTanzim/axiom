package io.axiom.persistence.tx;

/**
 * Defines how to handle existing transactions when a new transaction is requested.
 *
 * @since 0.1.0
 */
public enum Propagation {

    /**
     * Support a current transaction; create a new one if none exists.
     * This is the default propagation behavior.
     */
    REQUIRED,

    /**
     * Create a new transaction, suspending the current transaction if one exists.
     */
    REQUIRES_NEW,

    /**
     * Support a current transaction; execute non-transactionally if none exists.
     */
    SUPPORTS,

    /**
     * Do not support a current transaction; throw an exception if one exists.
     */
    NOT_SUPPORTED,

    /**
     * Support a current transaction; throw an exception if none exists.
     */
    MANDATORY,

    /**
     * Execute non-transactionally; throw an exception if a transaction exists.
     */
    NEVER,

    /**
     * Execute within a nested transaction if a current transaction exists,
     * behave like REQUIRED otherwise.
     */
    NESTED
}
