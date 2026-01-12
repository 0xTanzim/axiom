package io.axiom.core.json;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

/**
 * Jackson-based JSON codec implementation.
 *
 * <p>
 * This is the default {@link JsonCodec} implementation for Axiom,
 * using Jackson for high-performance JSON processing.
 *
 * <h2>Configuration</h2>
 * <p>
 * The default ObjectMapper is configured with sensible defaults:
 * <ul>
 * <li>Unknown properties are ignored (forward compatibility)</li>
 * <li>Null values are excluded from output</li>
 * <li>Dates are written as ISO-8601 strings</li>
 * </ul>
 *
 * <h2>Customization</h2>
 * <p>
 * For custom configuration, create with your own ObjectMapper:
 *
 * <pre>{@code
 * ObjectMapper mapper = new ObjectMapper();
 * mapper.enable(SerializationFeature.INDENT_OUTPUT);
 * JsonCodec codec = new JacksonCodec(mapper);
 * }</pre>
 *
 * @since 0.1.0
 */
public final class JacksonCodec implements JsonCodec {

    private final ObjectMapper mapper;

    /**
     * Creates a Jackson codec with default configuration.
     */
    public JacksonCodec() {
        this(JacksonCodec.createDefaultMapper());
    }

    /**
     * Creates a Jackson codec with custom ObjectMapper.
     *
     * @param mapper the ObjectMapper to use
     */
    public JacksonCodec(final ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "ObjectMapper cannot be null");
    }

    @Override
    public String serialize(final Object value) {
        try {
            return this.mapper.writeValueAsString(value);
        } catch (final JsonProcessingException e) {
            throw new JsonException("Failed to serialize object to JSON", e);
        }
    }

    @Override
    public byte[] serializeToBytes(final Object value) {
        try {
            return this.mapper.writeValueAsBytes(value);
        } catch (final JsonProcessingException e) {
            throw new JsonException("Failed to serialize object to JSON bytes", e);
        }
    }

    @Override
    public <T> T deserialize(final String json, final Class<T> type) {
        Objects.requireNonNull(json, "JSON string cannot be null");
        Objects.requireNonNull(type, "Target type cannot be null");
        try {
            return this.mapper.readValue(json, type);
        } catch (final JsonProcessingException e) {
            throw new JsonException("Failed to deserialize JSON to " + type.getName(), type, e);
        }
    }

    @Override
    public <T> T deserialize(final byte[] json, final Class<T> type) {
        Objects.requireNonNull(json, "JSON bytes cannot be null");
        Objects.requireNonNull(type, "Target type cannot be null");
        try {
            return this.mapper.readValue(json, type);
        } catch (final Exception e) {
            throw new JsonException("Failed to deserialize JSON bytes to " + type.getName(), type, e);
        }
    }

    /**
     * Returns the underlying ObjectMapper.
     *
     * <p>
     * Useful for advanced configuration or direct access.
     *
     * @return the ObjectMapper instance
     */
    public ObjectMapper mapper() {
        return this.mapper;
    }

    private static ObjectMapper createDefaultMapper() {
        final ObjectMapper mapper = new ObjectMapper();

        // Ignore unknown properties for forward compatibility
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Exclude null values from output
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Write dates as ISO-8601 strings
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        return mapper;
    }
}
