package io.axiom.persistence.config;

/**
 * Exception thrown when persistence configuration is invalid or cannot be loaded.
 *
 * @since 0.1.0
 */
public final class PersistenceConfigException extends RuntimeException {

    public PersistenceConfigException(String message) {
        super(message);
    }

    public PersistenceConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
