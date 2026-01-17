package io.axiom.config;

/**
 * Configuration exceptions for Axiom config system.
 *
 * <p>All configuration errors are runtime exceptions because:
 * <ul>
 *   <li>Configuration errors are typically unrecoverable</li>
 *   <li>Fail-fast at startup is preferred</li>
 *   <li>Cleaner API without checked exceptions</li>
 * </ul>
 */
public class ConfigException extends RuntimeException {

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Thrown when a required configuration key is missing.
     */
    public static class Missing extends ConfigException {
        private final String key;

        public Missing(String key) {
            super("Required configuration key not found: " + key);
            this.key = key;
        }

        /**
         * Returns the missing configuration key.
         *
         * @return the key that was not found
         */
        public String key() {
            return key;
        }
    }

    /**
     * Thrown when a configuration value cannot be converted to the requested type.
     */
    public static class WrongType extends ConfigException {
        private final String key;
        private final String expectedType;
        private final String actualValue;

        public WrongType(String key, String expectedType, String actualValue) {
            super("Configuration key '" + key + "' with value '" + actualValue + "' cannot be converted to " + expectedType);
            this.key = key;
            this.expectedType = expectedType;
            this.actualValue = actualValue;
        }

        public WrongType(String key, String expectedType, String actualValue, Throwable cause) {
            super("Configuration key '" + key + "' with value '" + actualValue + "' cannot be converted to " + expectedType, cause);
            this.key = key;
            this.expectedType = expectedType;
            this.actualValue = actualValue;
        }

        public String key() {
            return key;
        }

        public String expectedType() {
            return expectedType;
        }

        public String actualValue() {
            return actualValue;
        }
    }

    /**
     * Thrown when binding configuration to a mapping interface fails.
     */
    public static class BindingFailed extends ConfigException {
        private final Class<?> mappingClass;

        public BindingFailed(Class<?> mappingClass, Throwable cause) {
            super("Failed to bind configuration to " + mappingClass.getSimpleName() + ": " + cause.getMessage(), cause);
            this.mappingClass = mappingClass;
        }

        public Class<?> mappingClass() {
            return mappingClass;
        }
    }
}
