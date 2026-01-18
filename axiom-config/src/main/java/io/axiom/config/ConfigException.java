package io.axiom.config;

/**
 * Configuration exceptions with human-readable messages.
 *
 * <p>All configuration errors are runtime exceptions because:
 * <ul>
 *   <li>Configuration errors are typically unrecoverable</li>
 *   <li>Fail-fast at startup is preferred</li>
 *   <li>Cleaner API without checked exceptions</li>
 * </ul>
 *
 * <p>Error messages tell users exactly what to do to fix the problem.
 */
public class ConfigException extends RuntimeException {

    public ConfigException(final String message) {
        super(message);
    }

    public ConfigException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Thrown when a required configuration key is missing.
     * Provides actionable guidance on how to fix it.
     */
    public static class Missing extends ConfigException {
        private final String key;

        public Missing(final String key) {
            super(Missing.formatMissingMessage(key));
            this.key = key;
        }

        public String key() {
            return this.key;
        }

        private static String formatMissingMessage(final String key) {
            final String envVar = key.toUpperCase().replace('.', '_').replace('-', '_');
            return """

                ┌─────────────────────────────────────────────────────┐
                │  Configuration Error                                │
                ├─────────────────────────────────────────────────────┤
                │  Missing: %s
                │                                                     │
                │  Add to application.properties:                     │
                │    %s=<your-value>
                │                                                     │
                │  Or set environment variable:                       │
                │    export %s=<your-value>
                └─────────────────────────────────────────────────────┘
                """.formatted(key, key, envVar);
        }
    }

    /**
     * Thrown when a configuration value cannot be converted to the requested type.
     */
    public static class WrongType extends ConfigException {
        private final String key;
        private final String expectedType;
        private final String actualValue;

        public WrongType(final String key, final String expectedType, final String actualValue) {
            super(WrongType.formatTypeMessage(key, expectedType, actualValue));
            this.key = key;
            this.expectedType = expectedType;
            this.actualValue = actualValue;
        }

        public WrongType(final String key, final String expectedType, final String actualValue, final Throwable cause) {
            super(WrongType.formatTypeMessage(key, expectedType, actualValue), cause);
            this.key = key;
            this.expectedType = expectedType;
            this.actualValue = actualValue;
        }

        public String key() {
            return this.key;
        }

        public String expectedType() {
            return this.expectedType;
        }

        public String actualValue() {
            return this.actualValue;
        }

        private static String formatTypeMessage(final String key, final String expected, final String actual) {
            return """

                ┌─────────────────────────────────────────────────────┐
                │  Configuration Error                                │
                ├─────────────────────────────────────────────────────┤
                │  Key: %s
                │  Expected: %s
                │  Got: "%s"
                │                                                     │
                │  Please provide a valid %s value.
                └─────────────────────────────────────────────────────┘
                """.formatted(key, expected, actual, expected.toLowerCase());
        }
    }

    /**
     * Thrown when binding configuration to a mapping interface fails.
     */
    public static class BindingFailed extends ConfigException {
        private final Class<?> mappingClass;

        public BindingFailed(final Class<?> mappingClass, final Throwable cause) {
            super("Failed to bind configuration to " + mappingClass.getSimpleName() + ": " + cause.getMessage(), cause);
            this.mappingClass = mappingClass;
        }

        public Class<?> mappingClass() {
            return this.mappingClass;
        }
    }
}
