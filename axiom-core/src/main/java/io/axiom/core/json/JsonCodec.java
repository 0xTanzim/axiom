package io.axiom.core.json;

/**
 * JSON serialization and deserialization contract.
 *
 * <p>
 * This interface abstracts JSON processing from the framework core,
 * allowing different JSON libraries (Jackson, Gson, etc.) to be used
 * as runtime implementations.
 *
 * <h2>Design Rationale</h2>
 * <ul>
 * <li>Framework does not depend on specific JSON library</li>
 * <li>Users can swap implementations based on preference</li>
 * <li>Default implementation uses Jackson for broad compatibility</li>
 * </ul>
 *
 * @since 0.1.0
 */
public interface JsonCodec {

    /**
     * Serializes an object to JSON string.
     *
     * @param value the object to serialize
     * @return JSON string representation
     * @throws JsonException if serialization fails
     */
    String serialize(Object value);

    /**
     * Serializes an object to JSON bytes.
     *
     * @param value the object to serialize
     * @return JSON bytes (UTF-8 encoded)
     * @throws JsonException if serialization fails
     */
    byte[] serializeToBytes(Object value);

    /**
     * Deserializes JSON string to object.
     *
     * @param <T>   the target type
     * @param json  the JSON string
     * @param type  the target class
     * @return deserialized object
     * @throws JsonException if deserialization fails
     */
    <T> T deserialize(String json, Class<T> type);

    /**
     * Deserializes JSON bytes to object.
     *
     * @param <T>   the target type
     * @param json  the JSON bytes (UTF-8 encoded)
     * @param type  the target class
     * @return deserialized object
     * @throws JsonException if deserialization fails
     */
    <T> T deserialize(byte[] json, Class<T> type);
}
