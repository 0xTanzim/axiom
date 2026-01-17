package io.axiom.persistence.jooq;

/**
 * Exception thrown when a jOOQ operation fails.
 *
 * @since 0.1.0
 */
public final class JooqException extends RuntimeException {

    public JooqException(String message) {
        super(message);
    }

    public JooqException(String message, Throwable cause) {
        super(message, cause);
    }
}
