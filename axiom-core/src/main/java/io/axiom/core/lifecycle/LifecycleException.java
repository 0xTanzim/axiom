package io.axiom.core.lifecycle;

import io.axiom.core.error.*;

/**
 * Exception thrown when a lifecycle operation fails.
 *
 * @since 0.1.0
 */
public class LifecycleException extends AxiomException {

    private final LifecyclePhase failedPhase;

    public LifecycleException(final String message, final LifecyclePhase failedPhase) {
        super(message);
        this.failedPhase = failedPhase;
    }

    public LifecycleException(final String message, final Throwable cause, final LifecyclePhase failedPhase) {
        super(message, cause);
        this.failedPhase = failedPhase;
    }

    /**
     * Returns the lifecycle phase where the failure occurred.
     *
     * @return the failed phase
     */
    public LifecyclePhase failedPhase() {
        return this.failedPhase;
    }
}
