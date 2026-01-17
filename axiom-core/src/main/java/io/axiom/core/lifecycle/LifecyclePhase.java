package io.axiom.core.lifecycle;

/**
 * Lifecycle phases for Axiom applications.
 *
 * <p>
 * The lifecycle follows this state machine:
 * <pre>
 * INIT → STARTING → STARTED → STOPPING → STOPPED
 *   ↘       ↓          ↓          ↓
 *        ERROR ←──────────────────┘
 * </pre>
 *
 * @since 0.1.0
 */
public enum LifecyclePhase {

    /**
     * Initial state after construction.
     * Configuration can be modified.
     * No resources allocated.
     */
    INIT,

    /**
     * Transition state during startup.
     * OnStart hooks are executing.
     * Server is binding to port.
     */
    STARTING,

    /**
     * Normal running state.
     * Server is accepting requests.
     * OnReady hooks have completed.
     */
    STARTED,

    /**
     * Transition state during shutdown.
     * No new connections accepted.
     * Draining in-flight requests.
     * OnShutdown hooks are executing.
     */
    STOPPING,

    /**
     * Terminal state after graceful shutdown.
     * All resources released.
     * Can transition back to INIT via restart.
     */
    STOPPED,

    /**
     * Error state after unrecoverable failure.
     * OnError hooks have executed.
     * Requires restart for recovery.
     */
    ERROR
}
