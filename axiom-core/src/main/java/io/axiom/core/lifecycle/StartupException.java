package io.axiom.core.lifecycle;

/**
 * Exception thrown when server startup fails.
 *
 * @since 0.1.0
 */
public final class StartupException extends LifecycleException {

    public StartupException(final String message) {
        super(message, LifecyclePhase.STARTING);
    }

    public StartupException(final String message, final Throwable cause) {
        super(message, cause, LifecyclePhase.STARTING);
    }
}
