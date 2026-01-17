package io.axiom.persistence.jpa;

/**
 * Exception thrown when a JPA operation fails.
 *
 * @since 0.1.0
 */
public final class JpaException extends RuntimeException {

    public JpaException(String message) {
        super(message);
    }

    public JpaException(String message, Throwable cause) {
        super(message, cause);
    }
}
