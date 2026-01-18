package io.axiom.core.app;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.slf4j.*;

import io.axiom.core.routing.*;
import io.axiom.core.server.*;

/**
 * Factory for creating Axiom application instances.
 *
 * <p>Follows RFC-0001 and RFC-0002 patterns exactly.
 *
 * <h2>Style 1: Auto-Discovery with @Routes (Recommended)</h2>
 * <pre>{@code
 * var services = Services.create(config);
 * Axiom.start(services, 8080);  // Auto-discovers @Routes!
 * }</pre>
 *
 * <h2>Style 2: Simple Router (For Small Apps)</h2>
 * <pre>{@code
 * Router router = new Router();
 * router.get("/health", c -> c.text("OK"));
 * Axiom.start(router, 8080);
 * }</pre>
 *
 * <h2>Style 3: Full Control (RFC-0001 style)</h2>
 * <pre>{@code
 * App app = Axiom.create();
 * app.use((ctx, next) -> { log(ctx.path()); next.run(); });
 * app.route("/users", UserRoutes::router);
 * app.onError((ctx, e) -> { ... });
 * app.listen(8080);
 * }</pre>
 *
 * @since 0.1.0
 */
public final class Axiom {

    private static final Logger LOG = LoggerFactory.getLogger(Axiom.class);

    private static volatile ServerFactory cachedFactory;

    private Axiom() {
        // Factory class
    }

    // ========== Auto-Discovery Start Methods ==========

    /**
     * Starts a server with auto-discovered routes.
     *
     * <p>This is the recommended way to start an Axiom application.
     * It inspects the provided object for properties/methods returning
     * types annotated with {@code @Routes} and auto-mounts them.
     *
     * <pre>{@code
     * public record Services(AuthRoutes auth, UserRoutes users) { ... }
     *
     * var services = Services.create(config);
     * Axiom.start(services, 8080);  // Auto-discovers @Routes("/auth"), @Routes("/users")
     * }</pre>
     *
     * @param routeSource object containing @Routes-annotated components
     * @param port        the port to listen on
     */
    public static void start(final Object routeSource, final int port) {
        Axiom.start(routeSource, "0.0.0.0", port);
    }

    /**
     * Starts a server with auto-discovered routes on specified host.
     *
     * @param routeSource object containing @Routes-annotated components
     * @param host        the host to bind to
     * @param port        the port to listen on
     */
    public static void start(final Object routeSource, final String host, final int port) {
        final App app = Axiom.create();
        Axiom.mountRoutesFrom(app, routeSource);
        app.listen(host, port);
    }

    // ========== Simple Router Start Methods ==========

    /**
     * Starts a server with a single router.
     *
     * <p>This is the simplest way to start an Axiom application:
     * <pre>{@code
     * Router router = new Router();
     * router.get("/health", c -> c.text("OK"));
     * Axiom.start(router, 8080);
     * }</pre>
     *
     * @param router the router with all routes
     * @param port   the port to listen on
     */
    public static void start(final Router router, final int port) {
        final App app = Axiom.create();
        app.route(router);
        app.listen(port);
    }

    /**
     * Starts a server with a single router on specified host.
     *
     * @param router the router with all routes
     * @param host   the host to bind to
     * @param port   the port to listen on
     */
    public static void start(final Router router, final String host, final int port) {
        final App app = Axiom.create();
        app.route(router);
        app.listen(host, port);
    }

    /**
     * Creates a new App instance (RFC-0001 pattern).
     *
     * <pre>{@code
     * App app = Axiom.create();
     * app.use((ctx, next) -> { ... });
     * app.route("/users", UserRoutes::router);
     * app.listen(8080);
     * }</pre>
     *
     * @return a new App instance
     */
    public static App create() {
        return new DefaultApp();
    }

    /**
     * Creates a new DefaultApp for testing or direct access.
     *
     * <p>
     * Use this when you need access to DefaultApp-specific methods
     * like {@code buildHandler()} for unit testing.
     *
     * @return a new DefaultApp instance
     */
    public static DefaultApp createDefault() {
        return new DefaultApp();
    }

    /**
     * Discovers and returns the best available server factory.
     *
     * <p>
     * Factories are discovered via {@link ServiceLoader} and the one
     * with highest priority is returned. Results are cached.
     *
     * @return the server factory with highest priority
     * @throws IllegalStateException if no server runtime is available
     */
    public static ServerFactory serverFactory() {
        if (Axiom.cachedFactory != null) {
            return Axiom.cachedFactory;
        }

        synchronized (Axiom.class) {
            if (Axiom.cachedFactory != null) {
                return Axiom.cachedFactory;
            }

            Axiom.cachedFactory = ServiceLoader.load(ServerFactory.class)
                    .stream()
                    .map(ServiceLoader.Provider::get)
                    .max(Comparator.comparingInt(ServerFactory::priority))
                    .orElseThrow(() -> new IllegalStateException(
                            "No Axiom server runtime found. " +
                            "Add 'io.axiom:axiom-server' to your dependencies."));

            return Axiom.cachedFactory;
        }
    }

    /**
     * Creates a new server instance using the discovered factory.
     *
     * @return a new server ready for configuration
     * @throws IllegalStateException if no server runtime is available
     */
    public static Server createServer() {
        return Axiom.serverFactory().create();
    }

    /**
     * Returns the name of the discovered server runtime.
     *
     * @return server name or "none" if not available
     */
    public static String serverName() {
        try {
            return Axiom.serverFactory().name();
        } catch (final IllegalStateException e) {
            return "none";
        }
    }

    // ========== Route Auto-Discovery ==========

    /**
     * Mounts routes discovered from the provided source object.
     *
     * <p>Inspects all public methods (including record accessors) and mounts
     * any that return objects with {@code @Routes} annotation.
     */
    private static void mountRoutesFrom(final App app, final Object source) {
        final List<RouteMount> mounts = new ArrayList<>();
        final Class<?> sourceClass = source.getClass();

        for (final Method method : sourceClass.getMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }

            final Class<?> returnType = method.getReturnType();
            final Annotation routesAnnotation = Axiom.findRoutesAnnotation(returnType);

            if (routesAnnotation != null) {
                try {
                    final String path = Axiom.getAnnotationValue(routesAnnotation, "value");
                    final int order = Axiom.getAnnotationOrder(routesAnnotation);

                    if (path == null || path.isEmpty()) {
                        continue;
                    }

                    final Object routesInstance = method.invoke(source);
                    if (routesInstance == null) {
                        continue;
                    }

                    final Method routerMethod = returnType.getMethod("router");
                    final Router router = (Router) routerMethod.invoke(routesInstance);

                    mounts.add(new RouteMount(path, router, order, returnType.getSimpleName()));
                } catch (final NoSuchMethodException e) {
                    Axiom.LOG.warn("@Routes class {} has no router() method", returnType.getName());
                } catch (final Exception e) {
                    Axiom.LOG.error("Failed to mount routes from {}: {}", returnType.getName(), e.getMessage());
                }
            }
        }

        mounts.sort(Comparator.comparingInt(RouteMount::order));

        for (final RouteMount mount : mounts) {
            app.route(mount.path(), mount.router());
            Axiom.LOG.info("Auto-mounted: {} -> {}", mount.path(), mount.className());
        }

        if (mounts.isEmpty()) {
            Axiom.LOG.warn("No @Routes components found in {}", sourceClass.getSimpleName());
        }
    }

    /**
     * Finds @Routes annotation via reflection (supports io.axiom.di.Routes).
     */
    private static Annotation findRoutesAnnotation(final Class<?> type) {
        for (final Annotation annotation : type.getAnnotations()) {
            if ("Routes".equals(annotation.annotationType().getSimpleName())) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * Extracts value from annotation via reflection.
     */
    private static String getAnnotationValue(final Annotation annotation, final String methodName) {
        try {
            final Method method = annotation.annotationType().getMethod(methodName);
            final Object value = method.invoke(annotation);
            return value instanceof String ? (String) value : null;
        } catch (final Exception e) {
            return null;
        }
    }

    /**
     * Extracts order from annotation via reflection.
     */
    private static int getAnnotationOrder(final Annotation annotation) {
        try {
            final Method method = annotation.annotationType().getMethod("order");
            final Object value = method.invoke(annotation);
            return value instanceof Integer ? (Integer) value : 0;
        } catch (final Exception e) {
            return 0;
        }
    }

    private record RouteMount(String path, Router router, int order, String className) {}
}
