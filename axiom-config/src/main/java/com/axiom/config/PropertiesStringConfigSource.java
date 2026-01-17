package com.axiom.config;

import io.smallrye.config.common.MapBackedConfigSource;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * ConfigSource that reads from a properties string.
 * Useful for testing and programmatic configuration.
 */
final class PropertiesStringConfigSource extends MapBackedConfigSource {

    private static final String NAME = "PropertiesString";
    private static final int ORDINAL = 200;

    PropertiesStringConfigSource(String propertiesContent) {
        super(NAME, parseProperties(propertiesContent), ORDINAL);
    }

    private static Map<String, String> parseProperties(String content) {
        Properties props = new Properties();
        try {
            props.load(new StringReader(content));
        } catch (IOException e) {
            throw new ConfigException("Failed to parse properties string", e);
        }

        Map<String, String> result = new HashMap<>();
        for (String name : props.stringPropertyNames()) {
            result.put(name, props.getProperty(name));
        }
        return result;
    }
}
