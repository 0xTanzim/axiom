package io.axiom.core.json;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;

@DisplayName("JacksonCodec")
class JacksonCodecTest {

    private JacksonCodec codec;

    @BeforeEach
    void setUp() {
        this.codec = new JacksonCodec();
    }

    @Nested
    @DisplayName("serialize()")
    class Serialize {

        @Test
        @DisplayName("serializes object to JSON string")
        void serializesObjectToJsonString() {
            final TestUser user = new TestUser("john", 25);
            final String json = JacksonCodecTest.this.codec.serialize(user);
            Assertions.assertThat(json).contains("\"name\":\"john\"");
            Assertions.assertThat(json).contains("\"age\":25");
        }

        @Test
        @DisplayName("excludes null values")
        void excludesNullValues() {
            final TestUser user = new TestUser(null, 25);
            final String json = JacksonCodecTest.this.codec.serialize(user);
            Assertions.assertThat(json).doesNotContain("name");
            Assertions.assertThat(json).contains("\"age\":25");
        }

        @Test
        @DisplayName("serializes to bytes")
        void serializesToBytes() {
            final TestUser user = new TestUser("john", 25);
            final byte[] bytes = JacksonCodecTest.this.codec.serializeToBytes(user);
            Assertions.assertThat(bytes).isNotNull();
            Assertions.assertThat(new String(bytes)).contains("\"name\":\"john\"");
        }
    }

    @Nested
    @DisplayName("deserialize()")
    class Deserialize {

        @Test
        @DisplayName("deserializes JSON string to object")
        void deserializesJsonStringToObject() {
            final String json = "{\"name\":\"john\",\"age\":25}";
            final TestUser user = JacksonCodecTest.this.codec.deserialize(json, TestUser.class);
            Assertions.assertThat(user.name).isEqualTo("john");
            Assertions.assertThat(user.age).isEqualTo(25);
        }

        @Test
        @DisplayName("ignores unknown properties")
        void ignoresUnknownProperties() {
            final String json = "{\"name\":\"john\",\"age\":25,\"unknown\":\"value\"}";
            final TestUser user = JacksonCodecTest.this.codec.deserialize(json, TestUser.class);
            Assertions.assertThat(user.name).isEqualTo("john");
        }

        @Test
        @DisplayName("deserializes bytes to object")
        void deserializesBytesToObject() {
            final byte[] json = "{\"name\":\"john\",\"age\":25}".getBytes();
            final TestUser user = JacksonCodecTest.this.codec.deserialize(json, TestUser.class);
            Assertions.assertThat(user.name).isEqualTo("john");
        }

        @Test
        @DisplayName("throws JsonException for invalid JSON")
        void throwsJsonExceptionForInvalidJson() {
            Assertions.assertThatThrownBy(() -> JacksonCodecTest.this.codec.deserialize("invalid", TestUser.class))
                    .isInstanceOf(JsonException.class);
        }

        @Test
        @DisplayName("rejects null inputs")
        void rejectsNullInputs() {
            Assertions.assertThatThrownBy(() -> JacksonCodecTest.this.codec.deserialize((String) null, TestUser.class))
                    .isInstanceOf(NullPointerException.class);
            Assertions.assertThatThrownBy(() -> JacksonCodecTest.this.codec.deserialize("{}", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // Test data class
    public static class TestUser {
        public String name;
        public int age;

        public TestUser() {}

        public TestUser(final String name, final int age) {
            this.name = name;
            this.age = age;
        }
    }
}
