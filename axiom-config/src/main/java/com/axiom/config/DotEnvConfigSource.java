package com.axiom.config;

import io.smallrye.config.common.MapBackedConfigSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * ConfigSource that reads from a .env file.
 *
 * <p>.env files use the format:
 * <pre>
 * # Comment
 * KEY=value
 * DATABASE_URL=postgres://localhost/mydb
 * SECRET_KEY="quoted value"
 * </pre>
 *
 * <p>This source has ordinal 295, which is:
 * <ul>
 *   <li>Higher than application.properties (250)</li>
 *   <li>Lower than environment variables (300)</li>
 *   <li>Lower than system properties (400)</li>
 * </ul>
 */
public final class DotEnvConfigSource extends MapBackedConfigSource {

    private static final String NAME = "DotEnvFile";
    private static final int ORDINAL = 295;

    /**
     * Creates a .env config source from a file path.
     *
     * @param path the path to the .env file
     */
    public DotEnvConfigSource(Path path) {
        super(NAME, parseDotEnv(path), ORDINAL);
    }

    /**
     * Attempts to load .env from the current directory.
     * Returns empty map if file doesn't exist.
     *
     * @return the config source
     */
    public static DotEnvConfigSource fromCurrentDirectory() {
        return new DotEnvConfigSource(Path.of(".env"));
    }

    private static Map<String, String> parseDotEnv(Path path) {
        Map<String, String> result = new HashMap<>();

        if (!Files.exists(path)) {
            return result;
        }

        try {
            for (String line : Files.readAllLines(path)) {
                parseLine(line, result);
            }
        } catch (IOException e) {
            throw new ConfigException("Failed to read .env file: " + path, e);
        }

        return result;
    }

    private static void parseLine(String line, Map<String, String> result) {
        String trimmed = line.trim();

        // Skip empty lines and comments
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return;
        }

        int equalsIndex = trimmed.indexOf('=');
        if (equalsIndex <= 0) {
            return;
        }

        String key = trimmed.substring(0, equalsIndex).trim();
        String value = trimmed.substring(equalsIndex + 1).trim();

        // Remove quotes if present
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1);
        }

        // Convert KEY_NAME to key.name for consistency
        String normalizedKey = normalizeKey(key);
        result.put(normalizedKey, value);
    }

    /**
     * Converts environment variable style keys to property style.
     * DATABASE_URL → database.url
     */
    private static String normalizeKey(String key) {
        // If it's already lowercase with dots, keep it
        if (key.equals(key.toLowerCase()) && key.contains(".")) {
            return key;
        }

        // Convert UPPER_CASE to lower.case
        return key.toLowerCase().replace('_', '.');
    }
}
