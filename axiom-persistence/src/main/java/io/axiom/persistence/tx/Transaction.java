package io.axiom.persistence.tx;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Objects;
import java.util.concurrent.Callable;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main API for programmatic transaction management.
 *
 * <p>
 * This class provides the explicit transaction API that works without
 * annotation processing. Use this when you need fine-grained control
 * over transaction boundaries.
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * // Simple transaction
 * Transaction.execute(dataSource, () -> {
 *     // All operations here run in a transaction
 *     repository.save(entity);
 *     repository.update(other);
 * });
 *
 * // With return value
 * var result = Transaction.execute(dataSource, () -> {
 *     return repository.findById(id);
 * });
 *
 * // With options
 * Transaction.builder(dataSource)
 *     .isolation(IsolationLevel.SERIALIZABLE)
 *     .readOnly(true)
 *     .name("load-report")
 *     .execute(() -> loadReport());
 * }</pre>
 *
 * <h2>Accessing the Transaction</h2>
 * <pre>{@code
 * Transaction.execute(dataSource, () -> {
 *     // Get current context
 *     var ctx = TransactionContext.require();
 *
 *     // Mark for rollback
 *     ctx.setRollbackOnly();
 *
 *     // Get connection
 *     var conn = ctx.connection();
 * });
 * }</pre>
 *
 * @since 0.1.0
 */
public final class Transaction {

    private static final Logger LOG = LoggerFactory.getLogger(Transaction.class);

    private Transaction() {}

    // ==================== Simplified API (Global DataSource) ====================

    /**
     * Executes a runnable within a transaction using the global DataSource.
     *
     * <p>
     * Requires {@code AxiomPersistence.start()} to have been called first.
     *
     * <pre>{@code
     * AxiomPersistence.start();
     *
     * Transaction.execute(() -> {
     *     Jdbc.update("INSERT INTO users...", params);
     * });
     * }</pre>
     *
     * @param action the action to execute
     * @throws TransactionException if transaction fails
     * @throws io.axiom.persistence.PersistenceException if not started
     */
    public static void execute(TransactionalRunnable action) {
        execute(io.axiom.persistence.AxiomPersistence.globalDataSource(), action);
    }

    /**
     * Executes a callable within a transaction using the global DataSource.
     *
     * <pre>{@code
     * var user = Transaction.execute(() -> {
     *     return Jdbc.queryOne("SELECT * FROM users WHERE id = ?", mapper, id);
     * });
     * }</pre>
     *
     * @param <T> the return type
     * @param action the action to execute
     * @return the result of the action
     * @throws TransactionException if transaction fails
     * @throws io.axiom.persistence.PersistenceException if not started
     */
    public static <T> T execute(TransactionalCallable<T> action) {
        return execute(io.axiom.persistence.AxiomPersistence.globalDataSource(), action);
    }

    /**
     * Creates a transaction builder using the global DataSource.
     *
     * <pre>{@code
     * Transaction.builder()
     *     .isolation(IsolationLevel.SERIALIZABLE)
     *     .readOnly(true)
     *     .execute(() -> loadReport());
     * }</pre>
     *
     * @return transaction builder
     * @throws io.axiom.persistence.PersistenceException if not started
     */
    public static Builder builder() {
        return builder(io.axiom.persistence.AxiomPersistence.globalDataSource());
    }

    // ==================== Explicit DataSource API ====================

    /**
     * Executes a runnable within a transaction.
     *
     * @param dataSource the data source
     * @param action the action to execute
     * @throws TransactionException if transaction fails
     */
    public static void execute(DataSource dataSource, TransactionalRunnable action) {
        builder(dataSource).execute(action);
    }

    /**
     * Executes a callable within a transaction.
     *
     * @param <T> the return type
     * @param dataSource the data source
     * @param action the action to execute
     * @return the result of the action
     * @throws TransactionException if transaction fails
     */
    public static <T> T execute(DataSource dataSource, TransactionalCallable<T> action) {
        return builder(dataSource).execute(action);
    }

    // ==================== Builder API ====================

    /**
     * Creates a transaction builder with the given data source.
     *
     * @param dataSource the data source
     * @return transaction builder
     */
    public static Builder builder(DataSource dataSource) {
        return new Builder(dataSource);
    }

    /**
     * Builder for configuring transaction options.
     */
    public static final class Builder {
        private final DataSource dataSource;
        private IsolationLevel isolation = IsolationLevel.DEFAULT;
        private Propagation propagation = Propagation.REQUIRED;
        private boolean readOnly = false;
        private String name;
        private int timeoutSeconds = -1;
        private Class<? extends Throwable>[] rollbackFor;
        private Class<? extends Throwable>[] noRollbackFor;

        Builder(DataSource dataSource) {
            this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        }

        /**
         * Sets the transaction isolation level.
         *
         * @param isolation the isolation level
         * @return this builder
         */
        public Builder isolation(IsolationLevel isolation) {
            this.isolation = Objects.requireNonNull(isolation, "isolation");
            return this;
        }

        /**
         * Sets the transaction propagation behavior.
         *
         * @param propagation the propagation behavior
         * @return this builder
         */
        public Builder propagation(Propagation propagation) {
            this.propagation = Objects.requireNonNull(propagation, "propagation");
            return this;
        }

        /**
         * Marks the transaction as read-only.
         *
         * @param readOnly true for read-only
         * @return this builder
         */
        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        /**
         * Sets a name for the transaction (for logging/debugging).
         *
         * @param name the transaction name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the transaction timeout in seconds.
         *
         * @param seconds timeout in seconds (-1 for no timeout)
         * @return this builder
         */
        public Builder timeout(int seconds) {
            this.timeoutSeconds = seconds;
            return this;
        }

        /**
         * Sets exception types that should trigger rollback.
         *
         * @param exceptionTypes exception types
         * @return this builder
         */
        @SafeVarargs
        public final Builder rollbackFor(Class<? extends Throwable>... exceptionTypes) {
            this.rollbackFor = exceptionTypes;
            return this;
        }

        /**
         * Sets exception types that should NOT trigger rollback.
         *
         * @param exceptionTypes exception types
         * @return this builder
         */
        @SafeVarargs
        public final Builder noRollbackFor(Class<? extends Throwable>... exceptionTypes) {
            this.noRollbackFor = exceptionTypes;
            return this;
        }

        /**
         * Executes a runnable within the configured transaction.
         *
         * @param action the action to execute
         * @throws TransactionException if transaction fails
         */
        public void execute(TransactionalRunnable action) {
            execute(() -> {
                action.run();
                return null;
            });
        }

        /**
         * Executes a callable within the configured transaction.
         *
         * @param <T> the return type
         * @param action the action to execute
         * @return the result of the action
         * @throws TransactionException if transaction fails
         */
        public <T> T execute(TransactionalCallable<T> action) {
            return switch (propagation) {
                case REQUIRED -> executeRequired(action);
                case REQUIRES_NEW -> executeRequiresNew(action);
                case SUPPORTS -> executeSupports(action);
                case NOT_SUPPORTED -> executeNotSupported(action);
                case MANDATORY -> executeMandatory(action);
                case NEVER -> executeNever(action);
                case NESTED -> executeNested(action);
            };
        }

        private <T> T executeRequired(TransactionalCallable<T> action) {
            if (TransactionContext.isActive()) {
                LOG.trace("Joining existing transaction");
                return runInCurrentTransaction(action);
            }
            return runInNewTransaction(action);
        }

        private <T> T executeRequiresNew(TransactionalCallable<T> action) {
            // Always create a new transaction, even if one exists
            // Note: Suspending existing transaction would require more complex handling
            return runInNewTransaction(action);
        }

        private <T> T executeSupports(TransactionalCallable<T> action) {
            if (TransactionContext.isActive()) {
                return runInCurrentTransaction(action);
            }
            return runWithoutTransaction(action);
        }

        private <T> T executeNotSupported(TransactionalCallable<T> action) {
            // Run without transaction (suspending would be complex)
            return runWithoutTransaction(action);
        }

        private <T> T executeMandatory(TransactionalCallable<T> action) {
            if (!TransactionContext.isActive()) {
                throw new TransactionException(
                    "Transaction propagation 'MANDATORY' requires an existing transaction"
                );
            }
            return runInCurrentTransaction(action);
        }

        private <T> T executeNever(TransactionalCallable<T> action) {
            if (TransactionContext.isActive()) {
                throw new TransactionException(
                    "Transaction propagation 'NEVER' does not allow existing transaction"
                );
            }
            return runWithoutTransaction(action);
        }

        private <T> T executeNested(TransactionalCallable<T> action) {
            if (!TransactionContext.isActive()) {
                return runInNewTransaction(action);
            }
            return runInNestedTransaction(action);
        }

        private <T> T runInNewTransaction(TransactionalCallable<T> action) {
            Connection conn = null;
            TransactionContext ctx = null;

            try {
                conn = dataSource.getConnection();
                conn.setAutoCommit(false);

                if (!isolation.isDefault()) {
                    conn.setTransactionIsolation(isolation.jdbcLevel());
                }
                if (readOnly) {
                    conn.setReadOnly(true);
                }

                ctx = new TransactionContext(dataSource, conn, isolation, readOnly, name);
                LOG.debug("Started transaction: {}", ctx.name());

                final TransactionContext finalCtx = ctx;
                T result = ScopedValue.where(TransactionContext.scopedValue(), finalCtx)
                        .call(() -> executeAction(action, finalCtx));

                if (!finalCtx.isRollbackOnly()) {
                    finalCtx.commit();
                } else {
                    finalCtx.rollback();
                }

                return result;

            } catch (Throwable e) {
                if (ctx != null) {
                    handleException(ctx, e);
                }
                throw wrapException(e);
            } finally {
                if (ctx != null) {
                    ctx.close();
                }
            }
        }

        private <T> T runInCurrentTransaction(TransactionalCallable<T> action) {
            TransactionContext ctx = TransactionContext.require();
            try {
                return action.call();
            } catch (Throwable e) {
                handleException(ctx, e);
                throw wrapException(e);
            }
        }

        private <T> T runWithoutTransaction(TransactionalCallable<T> action) {
            try {
                return action.call();
            } catch (Exception e) {
                throw wrapException(e);
            }
        }

        private <T> T runInNestedTransaction(TransactionalCallable<T> action) {
            TransactionContext ctx = TransactionContext.require();
            Savepoint savepoint = null;

            try {
                savepoint = ctx.connection().setSavepoint();
                return action.call();
            } catch (Throwable e) {
                if (savepoint != null) {
                    try {
                        ctx.connection().rollback(savepoint);
                    } catch (SQLException ex) {
                        e.addSuppressed(ex);
                    }
                }
                throw wrapException(e);
            } finally {
                if (savepoint != null) {
                    try {
                        ctx.connection().releaseSavepoint(savepoint);
                    } catch (SQLException ex) {
                        LOG.warn("Failed to release savepoint", ex);
                    }
                }
            }
        }

        private <T> T executeAction(TransactionalCallable<T> action, TransactionContext ctx) {
            try {
                return action.call();
            } catch (Throwable e) {
                handleException(ctx, e);
                throw wrapException(e);
            }
        }

        private void handleException(TransactionContext ctx, Throwable e) {
            if (shouldRollback(e)) {
                ctx.setRollbackOnly(e);
                try {
                    ctx.rollback();
                } catch (TransactionException ex) {
                    e.addSuppressed(ex);
                }
            }
        }

        private boolean shouldRollback(Throwable e) {
            // Check noRollbackFor first
            if (noRollbackFor != null) {
                for (Class<? extends Throwable> type : noRollbackFor) {
                    if (type.isInstance(e)) {
                        return false;
                    }
                }
            }

            // Check rollbackFor
            if (rollbackFor != null && rollbackFor.length > 0) {
                for (Class<? extends Throwable> type : rollbackFor) {
                    if (type.isInstance(e)) {
                        return true;
                    }
                }
                return false;
            }

            // Default: rollback for RuntimeException and Error
            return e instanceof RuntimeException || e instanceof Error;
        }

        private RuntimeException wrapException(Throwable e) {
            if (e instanceof RuntimeException re) {
                return re;
            }
            if (e instanceof Error err) {
                throw err;
            }
            return new TransactionException("Transaction failed", e);
        }
    }

    // ==================== Functional Interfaces ====================

    /**
     * A runnable that can throw exceptions.
     */
    @FunctionalInterface
    public interface TransactionalRunnable {
        void run() throws Exception;
    }

    /**
     * A callable that can throw exceptions.
     *
     * @param <T> the return type
     */
    @FunctionalInterface
    public interface TransactionalCallable<T> {
        T call() throws Exception;
    }
}
