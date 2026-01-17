package io.axiom.persistence.tx;

import java.lang.annotation.*;

/**
 * Marks a method as transactional.
 *
 * <p>
 * When using the Axiom annotation processor, methods annotated with
 * {@code @Transactional} will automatically be wrapped in a transaction.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * @Transactional
 * public void saveOrder(Order order) {
 *     orderRepository.save(order);
 *     inventoryService.decrementStock(order.items());
 * }
 *
 * @Transactional(readOnly = true)
 * public List<Order> findOrders(String customerId) {
 *     return orderRepository.findByCustomerId(customerId);
 * }
 *
 * @Transactional(
 *     isolation = IsolationLevel.SERIALIZABLE,
 *     rollbackFor = BusinessException.class
 * )
 * public void processPayment(Payment payment) {
 *     // ...
 * }
 * }</pre>
 *
 * <h2>Explicit API Alternative</h2>
 * <p>
 * If annotation processing is not available or not desired, use the explicit
 * {@link Transaction} API:
 * <pre>{@code
 * Transaction.execute(dataSource, () -> {
 *     orderRepository.save(order);
 *     inventoryService.decrementStock(order.items());
 * });
 * }</pre>
 *
 * @since 0.1.0
 * @see Transaction
 * @see TransactionContext
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Transactional {

    /**
     * The transaction isolation level.
     * Default is to use the database default.
     *
     * @return the isolation level
     */
    IsolationLevel isolation() default IsolationLevel.DEFAULT;

    /**
     * The transaction propagation behavior.
     * Default is REQUIRED (join existing or create new).
     *
     * @return the propagation behavior
     */
    Propagation propagation() default Propagation.REQUIRED;

    /**
     * Whether the transaction is read-only.
     * Setting this to true may allow for optimizations by the database.
     *
     * @return true if read-only
     */
    boolean readOnly() default false;

    /**
     * Transaction timeout in seconds.
     * Default is -1 (no timeout, use database default).
     *
     * @return timeout in seconds
     */
    int timeout() default -1;

    /**
     * Exception types that trigger rollback.
     * By default, transactions rollback on RuntimeException and Error.
     *
     * @return exception types to rollback for
     */
    Class<? extends Throwable>[] rollbackFor() default {};

    /**
     * Exception class names that trigger rollback.
     * Use when the exception class is not available at compile time.
     *
     * @return exception class names to rollback for
     */
    String[] rollbackForClassName() default {};

    /**
     * Exception types that should NOT trigger rollback.
     *
     * @return exception types to not rollback for
     */
    Class<? extends Throwable>[] noRollbackFor() default {};

    /**
     * Exception class names that should NOT trigger rollback.
     * Use when the exception class is not available at compile time.
     *
     * @return exception class names to not rollback for
     */
    String[] noRollbackForClassName() default {};

    /**
     * A qualifier for the transaction manager to use.
     * Useful when multiple data sources are configured.
     *
     * @return the transaction manager qualifier
     */
    String transactionManager() default "";

    /**
     * A label for the transaction (for logging/monitoring).
     *
     * @return the transaction label
     */
    String label() default "";
}
