package io.axiom.examples.auth.middleware;

import io.axiom.core.context.*;
import io.axiom.core.middleware.*;
import io.axiom.examples.auth.domain.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Rate limiting middleware example - limits requests per IP.
 *
 * <h2>Usage in Auto-Discovery Mode</h2>
 * <pre>{@code
 * @Middleware(order = 3)  // Early in chain
 * public class RateLimitMiddleware { ... }
 * }</pre>
 *
 * <h2>Usage in Manual Mode</h2>
 * <pre>{@code
 * app.use(new RateLimitMiddleware(100, 60));  // 100 requests per 60 seconds
 * }</pre>
 *
 * <h2>Inline Version</h2>
 * <pre>{@code
 * var limits = new ConcurrentHashMap<String, AtomicInteger>();
 * app.use((ctx, next) -> {
 *     String ip = ctx.header("X-Forwarded-For");
 *     if (ip == null) ip = "unknown";
 *
 *     var count = limits.computeIfAbsent(ip, k -> new AtomicInteger(0));
 *     if (count.incrementAndGet() > 100) {
 *         ctx.status(429);
 *         ctx.json(Map.of("error", "Too many requests"));
 *         return;
 *     }
 *     next.run();
 * });
 * }</pre>
 */
@io.axiom.di.Middleware(order = 3)
public class RateLimitMiddleware implements MiddlewareHandler {

    private final int maxRequests;
    private final int windowSeconds;
    private final ConcurrentHashMap<String, RequestCounter> counters = new ConcurrentHashMap<>();

    public RateLimitMiddleware() {
        this(100, 60);  // 100 requests per minute
    }

    public RateLimitMiddleware(int maxRequests, int windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;

        // Cleanup expired counters periodically
        startCleanupTask();
    }

    @Override
    public void handle(Context ctx, Next next) throws Exception {
        String clientId = getClientId(ctx);

        RequestCounter counter = counters.computeIfAbsent(clientId, k -> new RequestCounter());

        if (counter.isExpired(windowSeconds)) {
            counter.reset();
        }

        if (counter.incrementAndGet() > maxRequests) {
            ctx.status(429);
            ctx.header("Retry-After", String.valueOf(counter.secondsUntilReset(windowSeconds)));
            ctx.json(new ErrorResponse(429, "Too Many Requests", "Too many requests. Please try again later."));
            return;
        }

        // Add rate limit headers
        ctx.header("X-RateLimit-Limit", String.valueOf(maxRequests));
        ctx.header("X-RateLimit-Remaining", String.valueOf(maxRequests - counter.get()));

        next.run();
    }

    private String getClientId(Context ctx) {
        // Use X-Forwarded-For for proxied requests, fallback to remote address
        String forwarded = ctx.header("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return ctx.header("Host");  // Fallback
    }

    private void startCleanupTask() {
        Thread.ofVirtual().start(() -> {
            while (true) {
                try {
                    Thread.sleep(windowSeconds * 1000L);
                    counters.entrySet().removeIf(e -> e.getValue().isExpired(windowSeconds * 2));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    private static class RequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        int incrementAndGet() {
            return count.incrementAndGet();
        }

        int get() {
            return count.get();
        }

        boolean isExpired(int windowSeconds) {
            return System.currentTimeMillis() - windowStart > windowSeconds * 1000L;
        }

        void reset() {
            count.set(0);
            windowStart = System.currentTimeMillis();
        }

        int secondsUntilReset(int windowSeconds) {
            long elapsed = System.currentTimeMillis() - windowStart;
            return Math.max(0, windowSeconds - (int)(elapsed / 1000));
        }
    }
}
