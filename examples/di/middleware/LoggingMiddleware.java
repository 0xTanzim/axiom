package playground.di.middleware;

import io.axiom.core.context.Context;
import io.axiom.core.middleware.MiddlewareHandler;
import io.axiom.core.middleware.Next;
import io.axiom.di.Middleware;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logging middleware — logs all requests.
 *
 * @Middleware(order = 1) — runs first
 */
@Middleware(order = 1)
public class LoggingMiddleware implements MiddlewareHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingMiddleware.class);

    @Inject
    public LoggingMiddleware() {}

    @Override
    public void handle(Context ctx, Next next) throws Exception {
        long start = System.nanoTime();
        String method = ctx.method();
        String path = ctx.path();

        LOG.info("→ {} {}", method, path);

        try {
            next.run();
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            LOG.info("← {} {} ({}ms)", method, path, durationMs);
        }
    }
}
