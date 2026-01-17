
/**
 * Core abstractions for the Axiom web framework.
 *
 * <p>
 * This module contains the fundamental interfaces and types that
 * define Axiom's programming model. It has zero external dependencies
 * and provides:
 *
 * <ul>
 * <li>{@link io.axiom.core.handler.Handler} - Core request handler</li>
 * <li>{@link io.axiom.core.context.Context} - Request/response context</li>
 * <li>{@link io.axiom.core.routing.Router} - Route registration</li>
 * <li>{@link io.axiom.core.middleware.MiddlewareHandler} - Middleware API</li>
 * <li>{@link io.axiom.core.app.App} - Application interface</li>
 * <li>{@link io.axiom.core.error.AxiomException} - Exception hierarchy</li>
 * </ul>
 *
 * <h2>Module Structure</h2>
 * <p>
 * This module exports all public packages. Internal packages
 * (like routing.internal) are not exported and should not be
 * accessed directly.
 *
 * <h2>Dependencies</h2>
 * <p>
 * axiom-core has NO dependencies. It uses only JDK standard library.
 *
 * @since 0.1.0
 */
module io.axiom.core {
    // Jackson dependency for JSON processing
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;

    // Public API packages
    exports io.axiom.core.handler;
    exports io.axiom.core.context;
    exports io.axiom.core.routing;
    exports io.axiom.core.middleware;
    exports io.axiom.core.error;
    exports io.axiom.core.app;
    exports io.axiom.core.json;
    exports io.axiom.core.server;
    exports io.axiom.core.lifecycle;

    // SPI for server runtime discovery
    uses io.axiom.core.server.ServerFactory;

    // Internal packages - NOT exported
    // io.axiom.core.routing.internal is hidden
}
