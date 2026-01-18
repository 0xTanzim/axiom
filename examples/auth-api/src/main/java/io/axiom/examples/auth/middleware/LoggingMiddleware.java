package io.axiom.examples.auth.middleware;

import io.axiom.core.context.*;
import io.axiom.core.middleware.*;
import org.slf4j.*;

/**
 * Logging middleware example - logs all requests.
 *
 * <h2>Usage in Auto-Discovery Mode</h2>
 * <pre>{@code
 * @Middleware(order = 1)  // Runs first
 * public class LoggingMiddleware { ... }
 * }</pre>
 *
 * <h2>Usage in Manual Mode</h2>
 * <pre>{@code
 * app.use(new LoggingMiddleware());
 * // or inline:
 * app.use((ctx, next) -> {
 *     LOG.info("{} {}", ctx.method(), ctx.path());
 *     long start = System.currentTimeMillis();
 *     next.run();
 *     LOG.info("{} {} - {}ms", ctx.method(), ctx.path(), System.currentTimeMillis() - start);
 * });
 * }</pre>
 */
@io.axiom.di.Middleware(order = 1)
public class LoggingMiddleware implements MiddlewareHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingMiddleware.class);

    @Override
    public void handle(Context ctx, Next next) throws Exception {
        long start = System.nanoTime();
        String method = ctx.method();
        String path = ctx.path();

        LOG.info("→ {} {}", method, path);

        try {
            next.run();
        } finally {
            long duration = (System.nanoTime() - start) / 1_000_000; // ms
            LOG.info("← {} {} ({}ms)", method, path, duration);
        }
    }
}
