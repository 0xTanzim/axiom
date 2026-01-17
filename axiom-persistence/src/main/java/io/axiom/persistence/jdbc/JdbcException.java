package io.axiom.persistence.jdbc;

/**
 * Exception thrown when a JDBC operation fails.
 *
 * @since 0.1.0
 */
public final class JdbcException extends RuntimeException {

    public JdbcException(String message) {
        super(message);
    }

    public JdbcException(String message, Throwable cause) {
        super(message, cause);
    }
}
