package io.axiom.config;

import java.lang.reflect.*;
import java.time.*;
import java.util.*;

/**
 * Internal utility for binding configuration values to Java records.
 *
 * <p>This class uses reflection to:
 * <ol>
 *   <li>Read record component names</li>
 *   <li>Fetch corresponding config values with prefix</li>
 *   <li>Convert values to appropriate types</li>
 *   <li>Invoke the canonical constructor</li>
 * </ol>
 *
 * <p>Users never interact with this class directly.
 * They use {@link Config#bind(String, Class)}.
 */
final class RecordBinder {

    private RecordBinder() {}

    /**
     * Binds configuration values to a record instance.
     *
     * @param <T> the record type
     * @param config the configuration source
     * @param prefix the key prefix (e.g., "database")
     * @param recordType the record class
     * @return a new record instance
     * @throws ConfigException if binding fails
     */
    static <T extends Record> T bind(final AxiomConfig config, final String prefix, final Class<T> recordType) {
        if (!recordType.isRecord()) {
            throw new ConfigException("Config binding requires a record type, got: " + recordType.getName());
        }

        final RecordComponent[] components = recordType.getRecordComponents();
        final Class<?>[] paramTypes = new Class<?>[components.length];
        final Object[] values = new Object[components.length];
        final List<String> missingKeys = new ArrayList<>();

        final String normalizedPrefix = prefix.endsWith(".") ? prefix : prefix + ".";

        for (int i = 0; i < components.length; i++) {
            final RecordComponent component = components[i];
            paramTypes[i] = component.getType();
            final String key = normalizedPrefix + RecordBinder.camelToKebab(component.getName());

            try {
                values[i] = RecordBinder.getValue(config, key, component.getType(), component.getName());
            } catch (final ConfigException.Missing e) {
                missingKeys.add(key);
            }
        }

        if (!missingKeys.isEmpty()) {
            throw new ConfigException.Missing(
                "Missing required configuration for " + recordType.getSimpleName() + ": " +
                String.join(", ", missingKeys)
            );
        }

        try {
            final Constructor<T> constructor = recordType.getDeclaredConstructor(paramTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(values);
        } catch (final Exception e) {
            throw new ConfigException("Failed to create " + recordType.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    private static Object getValue(final AxiomConfig config, final String key, final Class<?> type, final String componentName) {
        // Try both kebab-case and original name
        final String altKey = key.replace(RecordBinder.camelToKebab(componentName), componentName);

        if (type == String.class) {
            return config.get(key)
                .or(() -> config.get(altKey))
                .orElseThrow(() -> new ConfigException.Missing(key));
        }

        if (type == int.class || type == Integer.class) {
            return config.getInt(key)
                .or(() -> config.getInt(altKey))
                .orElseThrow(() -> new ConfigException.Missing(key));
        }

        if (type == long.class || type == Long.class) {
            return config.getLong(key)
                .or(() -> config.getLong(altKey))
                .orElseThrow(() -> new ConfigException.Missing(key));
        }

        if (type == boolean.class || type == Boolean.class) {
            return config.getBoolean(key)
                .or(() -> config.getBoolean(altKey))
                .orElseThrow(() -> new ConfigException.Missing(key));
        }

        if (type == double.class || type == Double.class) {
            return config.getDouble(key)
                .or(() -> config.getDouble(altKey))
                .orElseThrow(() -> new ConfigException.Missing(key));
        }

        if (type == Duration.class) {
            return config.getDuration(key)
                .or(() -> config.getDuration(altKey))
                .orElseThrow(() -> new ConfigException.Missing(key));
        }

        throw new ConfigException("Unsupported config type: " + type.getName() + " for key: " + key);
    }

    /**
     * Converts camelCase to kebab-case.
     * poolSize → pool-size
     * maxConnections → max-connections
     */
    private static String camelToKebab(final String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('-');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
