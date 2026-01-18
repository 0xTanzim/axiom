package io.axiom.examples.auth.config;

import io.axiom.config.AxiomConfig;
import io.axiom.config.Config;

/**
 * Configuration example demonstrating BOTH the new simplified API
 * and the underlying AxiomConfig when needed.
 *
 * <h2>The Axiom Way (Recommended)</h2>
 * <pre>{@code
 * // Just use Config.get() - that's it!
 * String dbUrl = Config.get("database.url");
 * int port = Config.get("server.port", 8080);
 * }</pre>
 *
 * <h2>Config Sources (Automatic)</h2>
 * <p>Axiom reads from (highest priority first):
 * <ol>
 *   <li>System properties (-Dserver.port=9090)</li>
 *   <li>Environment variables (SERVER_PORT=9090)</li>
 *   <li>.env file (if present)</li>
 *   <li>application-{profile}.properties</li>
 *   <li>application.properties</li>
 * </ol>
 *
 * <h2>Run this example</h2>
 * <pre>
 * mvn exec:java -Dexec.mainClass=io.axiom.examples.auth.config.ConfigExample
 * </pre>
 */
public class ConfigExample {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("  Axiom Configuration Example");
        System.out.println("=".repeat(60));
        System.out.println();

        // ============================================================
        // NEW WAY: Static Config class (Recommended)
        // ============================================================
        System.out.println("▶ NEW WAY: Config.get() - Zero complexity");
        System.out.println("-".repeat(60));

        // String values
        String appName = Config.get("app.name", "Default App");
        String appVersion = Config.get("app.version", "0.0.0");
        System.out.println("  app.name     = " + appName);
        System.out.println("  app.version  = " + appVersion);

        // Integer values
        int serverPort = Config.get("server.port", 8080);
        int poolSize = Config.get("database.poolSize", 5);
        System.out.println("  server.port  = " + serverPort);
        System.out.println("  db.poolSize  = " + poolSize);

        // Boolean values
        boolean debug = Config.get("app.debug", false);
        boolean regEnabled = Config.get("features.registration.enabled", false);
        System.out.println("  app.debug    = " + debug);
        System.out.println("  registration = " + regEnabled);

        System.out.println();

        // ============================================================
        // Pre-built Framework Configs
        // ============================================================
        System.out.println("▶ PRE-BUILT CONFIGS: Config.server(), Config.database()");
        System.out.println("-".repeat(60));

        Config.ServerConfig server = Config.server();
        System.out.println("  Server:");
        System.out.println("    host        = " + server.host());
        System.out.println("    port        = " + server.port());
        System.out.println("    contextPath = " + server.contextPath());

        // Database requires database.url to be set
        try {
            Config.DatabaseConfig db = Config.database();
            System.out.println("  Database:");
            System.out.println("    url         = " + db.url());
            System.out.println("    username    = " + db.username());
            System.out.println("    poolSize    = " + db.poolSize());
        } catch (Exception e) {
            System.out.println("  Database: (not configured - " + e.getMessage() + ")");
        }

        System.out.println();

        // ============================================================
        // Record Binding
        // ============================================================
        System.out.println("▶ RECORD BINDING: Config.bind()");
        System.out.println("-".repeat(60));

        // Define a custom config record
        record JwtConfig(String secret, int expiration) {}

        try {
            JwtConfig jwt = Config.bind("jwt", JwtConfig.class);
            System.out.println("  JWT Config:");
            System.out.println("    secret     = " + jwt.secret().substring(0, 10) + "...");
            System.out.println("    expiration = " + jwt.expiration() + " seconds");
        } catch (Exception e) {
            System.out.println("  JWT Config: Not configured");
        }

        System.out.println();

        // ============================================================
        // OLD WAY: AxiomConfig (Still available for advanced use)
        // ============================================================
        System.out.println("▶ OLD WAY: AxiomConfig (for advanced scenarios)");
        System.out.println("-".repeat(60));

        AxiomConfig config = Config.raw();
        System.out.println("  Using AxiomConfig.get():");
        System.out.println("    server.host = " + config.get("server.host").orElse("not set"));
        System.out.println("    subset('app'):");

        AxiomConfig appConfig = config.subset("app");
        System.out.println("      name    = " + appConfig.get("name").orElse("not set"));
        System.out.println("      version = " + appConfig.get("version").orElse("not set"));

        System.out.println();

        // ============================================================
        // Environment Variable Mapping
        // ============================================================
        System.out.println("▶ ENVIRONMENT VARIABLE MAPPING");
        System.out.println("-".repeat(60));
        System.out.println("  Properties file keys are automatically mapped from env vars:");
        System.out.println("    server.port     ← SERVER_PORT");
        System.out.println("    database.url    ← DATABASE_URL");
        System.out.println("    app.debug       ← APP_DEBUG");
        System.out.println();
        System.out.println("  .env file is read automatically (for local dev)");

        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("  Configuration loaded successfully!");
        System.out.println("=".repeat(60));
    }
}
