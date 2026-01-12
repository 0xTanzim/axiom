package io.axiom.core.error;

/**
 * Exception thrown when attempting to modify an already-committed response.
 *
 * <p>
 * A response is committed after any body-writing method is called
 * ({@code text()}, {@code json()}, {@code send()}).
 *
 * <p>
 * This is a programming error, not a client error. It indicates
 * that handler or middleware code attempted to:
 * <ul>
 * <li>Set status after body was written</li>
 * <li>Set headers after body was written</li>
 * <li>Write body multiple times</li>
 * </ul>
 *
 * <h2>Example of Incorrect Code</h2>
 * 
 * <pre>{@code
 * // This will throw ResponseCommittedException
 * c.text("First response");
 * c.text("Second response"); // ERROR: response already committed
 *
 * // This will also throw
 * c.json(data);
 * c.status(201); // ERROR: cannot set status after body
 * }</pre>
 *
 * <h2>Correct Pattern</h2>
 * 
 * <pre>{@code
 * // Set all headers and status first
 * c.status(201);
 * c.header("X-Custom", "value");
 * // Then write body (commits response)
 * c.json(data);
 * }</pre>
 *
 * @since 0.1.0
 */
public final class ResponseCommittedException extends AxiomException {

    /**
     * Creates a response committed exception.
     */
    public ResponseCommittedException() {
        super("Response already committed. Cannot modify headers or write body again.");
    }

    /**
     * Creates a response committed exception with detail.
     *
     * @param operation the operation that was attempted
     */
    public ResponseCommittedException(String operation) {
        super("Cannot " + operation + ": response already committed");
    }
}
