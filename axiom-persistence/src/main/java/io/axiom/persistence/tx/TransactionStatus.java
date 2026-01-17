package io.axiom.persistence.tx;

/**
 * Transaction status indicating the current state of a transaction.
 *
 * @since 0.1.0
 */
public enum TransactionStatus {

    /**
     * Transaction is active and can accept operations.
     */
    ACTIVE,

    /**
     * Transaction is marked for rollback only.
     * It will be rolled back when it completes.
     */
    MARKED_ROLLBACK,

    /**
     * Transaction is currently committing.
     */
    COMMITTING,

    /**
     * Transaction has been successfully committed.
     */
    COMMITTED,

    /**
     * Transaction is currently rolling back.
     */
    ROLLING_BACK,

    /**
     * Transaction has been rolled back.
     */
    ROLLED_BACK,

    /**
     * Transaction status is unknown (error during commit/rollback).
     */
    UNKNOWN
}
