package io.axiom.core.lifecycle;

/**
 * A runnable that may throw checked exceptions.
 *
 * <p>
 * Used for lifecycle hooks that may perform I/O or other
 * operations that throw checked exceptions.
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface ThrowingRunnable {

    /**
     * Executes the action.
     *
     * @throws Exception if the action fails
     */
    void run() throws Exception;
}
