package io.axiom.persistence;

/**
 * General exception for persistence-related failures.
 *
 * @since 0.1.0
 */
public final class PersistenceException extends RuntimeException {

    public PersistenceException(String message) {
        super(message);
    }

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
