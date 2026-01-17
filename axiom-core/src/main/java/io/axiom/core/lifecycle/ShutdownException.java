package io.axiom.core.lifecycle;

import java.util.*;

/**
 * Exception thrown when server shutdown encounters errors.
 *
 * <p>
 * Contains all hook failures that occurred during shutdown.
 * Shutdown continues even if some hooks fail.
 *
 * @since 0.1.0
 */
public final class ShutdownException extends LifecycleException {

    private final List<Throwable> hookFailures;

    public ShutdownException(final String message, final List<Throwable> hookFailures) {
        super(message, LifecyclePhase.STOPPING);
        this.hookFailures = hookFailures != null ? List.copyOf(hookFailures) : List.of();
    }

    /**
     * Returns all exceptions thrown by shutdown hooks.
     *
     * @return immutable list of hook failures
     */
    public List<Throwable> hookFailures() {
        return this.hookFailures;
    }
}
