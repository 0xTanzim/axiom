package io.axiom.persistence.config;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads persistence configuration from properties files.
 *
 * <p>
 * Search order (first found wins):
 * <ol>
 *   <li>System property: {@code axiom.config.file}</li>
 *   <li>Environment variable: {@code AXIOM_CONFIG_FILE}</li>
 *   <li>Classpath: {@code application.properties}</li>
 *   <li>Working directory: {@code application.properties}</li>
 * </ol>
 *
 * <p>
 * Environment variables override properties file values.
 * Pattern: {@code AXIOM_DATASOURCE_URL} overrides {@code axiom.datasource.url}
 *
 * @since 0.1.0
 */
public final class PersistenceConfigLoader {

    private static final Logger LOG = LoggerFactory.getLogger(PersistenceConfigLoader.class);

    private static final String DEFAULT_FILE = "application.properties";
    private static final String CONFIG_FILE_PROPERTY = "axiom.config.file";
    private static final String CONFIG_FILE_ENV = "AXIOM_CONFIG_FILE";

    private PersistenceConfigLoader() {}

    /**
     * Loads configuration from default locations.
     *
     * @return loaded configuration
     * @throws PersistenceConfigException if no configuration found or invalid
     */
    public static PersistenceConfig load() {
        Properties props = loadProperties();
        applyEnvironmentOverrides(props);
        return PersistenceConfig.fromProperties(props);
    }

    /**
     * Loads configuration from specific file.
     *
     * @param path the file path
     * @return loaded configuration
     * @throws PersistenceConfigException if file not found or invalid
     */
    public static PersistenceConfig loadFrom(Path path) {
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(path)) {
            props.load(is);
            LOG.info("Loaded persistence config from: {}", path);
        } catch (IOException e) {
            throw new PersistenceConfigException("Failed to load config from: " + path, e);
        }
        applyEnvironmentOverrides(props);
        return PersistenceConfig.fromProperties(props);
    }

    private static Properties loadProperties() {
        // 1. Check system property
        String configFile = System.getProperty(CONFIG_FILE_PROPERTY);
        if (configFile != null && !configFile.isBlank()) {
            return loadFromPath(Path.of(configFile));
        }

        // 2. Check environment variable
        configFile = System.getenv(CONFIG_FILE_ENV);
        if (configFile != null && !configFile.isBlank()) {
            return loadFromPath(Path.of(configFile));
        }

        // 3. Try classpath
        Properties props = loadFromClasspath(DEFAULT_FILE);
        if (props != null) {
            return props;
        }

        // 4. Try working directory
        Path workingDir = Path.of(DEFAULT_FILE);
        if (Files.exists(workingDir)) {
            return loadFromPath(workingDir);
        }

        LOG.warn("No configuration file found, using defaults");
        return new Properties();
    }

    private static Properties loadFromPath(Path path) {
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(path)) {
            props.load(is);
            LOG.info("Loaded persistence config from: {}", path);
            return props;
        } catch (IOException e) {
            throw new PersistenceConfigException("Failed to load config from: " + path, e);
        }
    }

    private static Properties loadFromClasspath(String resource) {
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resource)) {
            if (is == null) {
                return null;
            }
            Properties props = new Properties();
            props.load(is);
            LOG.info("Loaded persistence config from classpath: {}", resource);
            return props;
        } catch (IOException e) {
            LOG.warn("Failed to load config from classpath: {}", resource, e);
            return null;
        }
    }

    private static void applyEnvironmentOverrides(Properties props) {
        Map<String, String> env = System.getenv();

        overrideFromEnv(props, env, PersistenceConfig.PROP_URL, "AXIOM_DATASOURCE_URL");
        overrideFromEnv(props, env, PersistenceConfig.PROP_USERNAME, "AXIOM_DATASOURCE_USERNAME");
        overrideFromEnv(props, env, PersistenceConfig.PROP_PASSWORD, "AXIOM_DATASOURCE_PASSWORD");
        overrideFromEnv(props, env, PersistenceConfig.PROP_DRIVER, "AXIOM_DATASOURCE_DRIVER");
        overrideFromEnv(props, env, PersistenceConfig.PROP_POOL_SIZE, "AXIOM_DATASOURCE_POOL_SIZE");
    }

    private static void overrideFromEnv(Properties props, Map<String, String> env,
                                        String propKey, String envKey) {
        String envValue = env.get(envKey);
        if (envValue != null && !envValue.isBlank()) {
            props.setProperty(propKey, envValue);
            LOG.debug("Overriding {} from environment", propKey);
        }
    }
}
