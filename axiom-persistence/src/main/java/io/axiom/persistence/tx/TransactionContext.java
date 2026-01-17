package io.axiom.persistence.tx;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.sql.Connection;
import java.util.Objects;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transaction context bound to the current scope using Scoped Values.
 *
 * <p>
 * This class manages transaction state for the current execution scope.
 * It uses Java 21+ Scoped Values instead of ThreadLocal for better
 * virtual thread support and cleaner scope semantics.
 *
 * <p>
 * Transaction contexts are:
 * <ul>
 *   <li>Immutably bound to a scope (cannot leak to other threads)</li>
 *   <li>Automatically cleaned up when scope exits</li>
 *   <li>Safe for virtual threads (no pinning)</li>
 * </ul>
 *
 * @since 0.1.0
 */
public final class TransactionContext {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionContext.class);

    /**
     * Scoped value holding the current transaction context.
     * Using Scoped Values (Java 21+) instead of ThreadLocal for:
     * - Better virtual thread support
     * - Cleaner inheritance semantics
     * - Guaranteed cleanup on scope exit
     */
    private static final ScopedValue<TransactionContext> CURRENT = ScopedValue.newInstance();

    private final DataSource dataSource;
    private final Connection connection;
    private final IsolationLevel isolationLevel;
    private final boolean readOnly;
    private final String name;

    // Mutable state managed atomically
    private volatile TransactionStatus status;
    private volatile Throwable rollbackCause;

    // VarHandle for atomic status updates
    private static final VarHandle STATUS_HANDLE;
    static {
        try {
            STATUS_HANDLE = MethodHandles.lookup()
                    .findVarHandle(TransactionContext.class, "status", TransactionStatus.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    TransactionContext(DataSource dataSource, Connection connection,
                       IsolationLevel isolationLevel, boolean readOnly, String name) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.connection = Objects.requireNonNull(connection, "connection");
        this.isolationLevel = Objects.requireNonNull(isolationLevel, "isolationLevel");
        this.readOnly = readOnly;
        this.name = name != null ? name : generateName();
        this.status = TransactionStatus.ACTIVE;
    }

    /**
     * Returns the current transaction context, if any.
     *
     * @return current context or empty if no active transaction
     */
    public static java.util.Optional<TransactionContext> current() {
        if (CURRENT.isBound()) {
            return java.util.Optional.of(CURRENT.get());
        }
        return java.util.Optional.empty();
    }

    /**
     * Returns the current transaction context.
     *
     * @return current context
     * @throws TransactionException if no active transaction
     */
    public static TransactionContext require() {
        if (!CURRENT.isBound()) {
            throw new TransactionException("No active transaction in current scope");
        }
        return CURRENT.get();
    }

    /**
     * Checks if there is an active transaction in the current scope.
     *
     * @return true if a transaction is active
     */
    public static boolean isActive() {
        return CURRENT.isBound() && CURRENT.get().status == TransactionStatus.ACTIVE;
    }

    /**
     * Returns the scoped value for transaction context binding.
     * Used internally by Transaction class.
     */
    static ScopedValue<TransactionContext> scopedValue() {
        return CURRENT;
    }

    /**
     * Returns the connection for this transaction.
     *
     * @return the JDBC connection
     */
    public Connection connection() {
        ensureActive();
        return connection;
    }

    /**
     * Returns the data source that created this connection.
     *
     * @return the data source
     */
    public DataSource dataSource() {
        return dataSource;
    }

    /**
     * Returns the isolation level for this transaction.
     *
     * @return the isolation level
     */
    public IsolationLevel isolationLevel() {
        return isolationLevel;
    }

    /**
     * Returns whether this transaction is read-only.
     *
     * @return true if read-only
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Returns the transaction name for logging/debugging.
     *
     * @return transaction name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the current transaction status.
     *
     * @return transaction status
     */
    public TransactionStatus status() {
        return status;
    }

    /**
     * Marks this transaction for rollback only.
     * The transaction will be rolled back when it completes.
     *
     * @param cause the reason for rollback (optional)
     */
    public void setRollbackOnly(Throwable cause) {
        if (STATUS_HANDLE.compareAndSet(this, TransactionStatus.ACTIVE, TransactionStatus.MARKED_ROLLBACK)) {
            this.rollbackCause = cause;
            LOG.debug("Transaction {} marked for rollback: {}", name, cause != null ? cause.getMessage() : "no cause");
        }
    }

    /**
     * Marks this transaction for rollback only.
     */
    public void setRollbackOnly() {
        setRollbackOnly(null);
    }

    /**
     * Returns whether this transaction is marked for rollback.
     *
     * @return true if marked for rollback
     */
    public boolean isRollbackOnly() {
        TransactionStatus s = status;
        return s == TransactionStatus.MARKED_ROLLBACK || s == TransactionStatus.ROLLED_BACK;
    }

    /**
     * Returns the cause of rollback, if any.
     *
     * @return rollback cause or null
     */
    public Throwable rollbackCause() {
        return rollbackCause;
    }

    // Internal methods for Transaction class

    void commit() throws TransactionException {
        if (status == TransactionStatus.MARKED_ROLLBACK) {
            rollback();
            throw new TransactionException("Transaction was marked for rollback", rollbackCause);
        }

        if (!STATUS_HANDLE.compareAndSet(this, TransactionStatus.ACTIVE, TransactionStatus.COMMITTING)) {
            throw new TransactionException("Cannot commit transaction in status: " + status);
        }

        try {
            connection.commit();
            status = TransactionStatus.COMMITTED;
            LOG.debug("Transaction {} committed", name);
        } catch (Exception e) {
            status = TransactionStatus.UNKNOWN;
            throw new TransactionException("Failed to commit transaction: " + name, e);
        }
    }

    void rollback() throws TransactionException {
        TransactionStatus current = status;
        if (current == TransactionStatus.COMMITTED || current == TransactionStatus.ROLLED_BACK) {
            return; // Already completed
        }

        status = TransactionStatus.ROLLING_BACK;
        try {
            connection.rollback();
            status = TransactionStatus.ROLLED_BACK;
            LOG.debug("Transaction {} rolled back", name);
        } catch (Exception e) {
            status = TransactionStatus.UNKNOWN;
            throw new TransactionException("Failed to rollback transaction: " + name, e);
        }
    }

    void close() {
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            LOG.warn("Failed to close connection for transaction {}: {}", name, e.getMessage());
        }
    }

    private void ensureActive() {
        if (status != TransactionStatus.ACTIVE && status != TransactionStatus.MARKED_ROLLBACK) {
            throw new TransactionException("Transaction is not active: " + status);
        }
    }

    private static String generateName() {
        return "tx-" + System.nanoTime();
    }

    @Override
    public String toString() {
        return "TransactionContext[name=" + name + ", status=" + status +
               ", isolation=" + isolationLevel + ", readOnly=" + readOnly + "]";
    }
}
