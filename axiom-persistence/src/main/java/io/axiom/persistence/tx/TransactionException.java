package io.axiom.persistence.tx;

/**
 * Exception thrown when a transaction operation fails.
 *
 * @since 0.1.0
 */
public final class TransactionException extends RuntimeException {

    public TransactionException(String message) {
        super(message);
    }

    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
