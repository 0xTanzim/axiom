
/**
 * Core Axiom framework module.
 *
 * <p>
 * This module contains the complete Axiom framework core including:
 *
 * <ul>
 * <li><b>Routing</b> - {@link io.axiom.core.routing.Router}, path matching</li>
 * <li><b>Context</b> - {@link io.axiom.core.context.Context} request/response</li>
 * <li><b>Middleware</b> - {@link io.axiom.core.middleware.MiddlewareHandler}</li>
 * <li><b>Config</b> - {@link io.axiom.config.Config} zero-headache configuration</li>
 * <li><b>DI</b> - {@link io.axiom.di.Service} compile-time dependency injection</li>
 * <li><b>Validation</b> - {@link io.axiom.validation.AxiomValidator}</li>
 * <li><b>Server</b> - {@link io.axiom.server.JdkServer} JDK HTTP server</li>
 * </ul>
 *
 * @since 0.1.0
 */
module io.axiom.core {
    // Logging
    requires org.slf4j;

    // JSON
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;

    // Configuration (automatic modules)
    requires smallrye.config;
    requires smallrye.config.common;

    // Dependency Injection (automatic module)
    requires dagger;
    requires jakarta.inject;

    // Validation
    requires jakarta.validation;
    requires org.hibernate.validator;

    // JDK HTTP Server
    requires jdk.httpserver;

    // ==================== Core API ====================
    exports io.axiom.core.handler;
    exports io.axiom.core.context;
    exports io.axiom.core.routing;
    exports io.axiom.core.middleware;
    exports io.axiom.core.error;
    exports io.axiom.core.app;
    exports io.axiom.core.json;
    exports io.axiom.core.server;
    exports io.axiom.core.lifecycle;

    // ==================== Config ====================
    exports io.axiom.config;

    // ==================== Dependency Injection ====================
    exports io.axiom.di;

    // ==================== Validation ====================
    exports io.axiom.validation;

    // ==================== Server Implementation ====================
    exports io.axiom.server;

    // SPI for server runtime discovery
    uses io.axiom.core.server.ServerFactory;
    provides io.axiom.core.server.ServerFactory with io.axiom.server.JdkServerFactory;
}
