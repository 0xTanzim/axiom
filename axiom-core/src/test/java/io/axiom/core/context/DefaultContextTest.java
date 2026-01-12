package io.axiom.core.context;

import java.nio.charset.*;
import java.util.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;

import io.axiom.core.error.*;
import io.axiom.core.json.*;

@DisplayName("DefaultContext")
class DefaultContextTest {

    private TestRequest request;
    private TestResponse response;
    private DefaultContext context;

    @BeforeEach
    void setUp() {
        this.request = new TestRequest();
        this.response = new TestResponse();
        this.context = new DefaultContext(this.request, this.response, new JacksonCodec());
    }

    @Nested
    @DisplayName("Request methods")
    class RequestMethods {

        @Test
        @DisplayName("method() returns HTTP method")
        void methodReturnsHttpMethod() {
            DefaultContextTest.this.request.method = "POST";
            Assertions.assertThat(DefaultContextTest.this.context.method()).isEqualTo("POST");
        }

        @Test
        @DisplayName("path() returns request path")
        void pathReturnsRequestPath() {
            DefaultContextTest.this.request.path = "/users/123";
            Assertions.assertThat(DefaultContextTest.this.context.path()).isEqualTo("/users/123");
        }

        @Test
        @DisplayName("param() returns path parameter")
        void paramReturnsPathParameter() {
            DefaultContextTest.this.request.params.put("id", "123");
            Assertions.assertThat(DefaultContextTest.this.context.param("id")).isEqualTo("123");
        }

        @Test
        @DisplayName("query() returns query parameter")
        void queryReturnsQueryParameter() {
            DefaultContextTest.this.request.queryParams.put("page", "2");
            Assertions.assertThat(DefaultContextTest.this.context.query("page")).isEqualTo("2");
        }

        @Test
        @DisplayName("headers() returns all headers")
        void headersReturnsAllHeaders() {
            DefaultContextTest.this.request.headers.put("Content-Type", "application/json");
            Assertions.assertThat(DefaultContextTest.this.context.headers()).containsEntry("Content-Type", "application/json");
        }
    }

    @Nested
    @DisplayName("Body parsing")
    class BodyParsing {

        @Test
        @DisplayName("body() parses JSON to object")
        void bodyParsesJsonToObject() {
            DefaultContextTest.this.request.body = "{\"name\":\"john\",\"age\":25}".getBytes(StandardCharsets.UTF_8);
            final TestUser user = DefaultContextTest.this.context.body(TestUser.class);
            Assertions.assertThat(user.name).isEqualTo("john");
            Assertions.assertThat(user.age).isEqualTo(25);
        }

        @Test
        @DisplayName("body() returns String for String.class")
        void bodyReturnsStringForStringClass() {
            DefaultContextTest.this.request.body = "plain text".getBytes(StandardCharsets.UTF_8);
            final String body = DefaultContextTest.this.context.body(String.class);
            Assertions.assertThat(body).isEqualTo("plain text");
        }

        @Test
        @DisplayName("body() returns raw bytes for byte[].class")
        void bodyReturnsRawBytes() {
            DefaultContextTest.this.request.body = "raw data".getBytes(StandardCharsets.UTF_8);
            final byte[] body = DefaultContextTest.this.context.body(byte[].class);
            Assertions.assertThat(body).isEqualTo("raw data".getBytes(StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("body() caches parsed result")
        void bodyCachesParsedResult() {
            DefaultContextTest.this.request.body = "{\"name\":\"john\",\"age\":25}".getBytes(StandardCharsets.UTF_8);
            final TestUser first = DefaultContextTest.this.context.body(TestUser.class);
            final TestUser second = DefaultContextTest.this.context.body(TestUser.class);
            Assertions.assertThat(first).isSameAs(second);
        }

        @Test
        @DisplayName("body() throws BodyParseException for invalid JSON")
        void bodyThrowsForInvalidJson() {
            DefaultContextTest.this.request.body = "invalid json".getBytes(StandardCharsets.UTF_8);
            Assertions.assertThatThrownBy(() -> DefaultContextTest.this.context.body(TestUser.class))
                    .isInstanceOf(BodyParseException.class);
        }
    }

    @Nested
    @DisplayName("Response methods")
    class ResponseMethods {

        @Test
        @DisplayName("status() sets response status")
        void statusSetsResponseStatus() {
            DefaultContextTest.this.context.status(201);
            Assertions.assertThat(DefaultContextTest.this.response.statusCode).isEqualTo(201);
        }

        @Test
        @DisplayName("header() sets response header")
        void headerSetsResponseHeader() {
            DefaultContextTest.this.context.header("X-Custom", "value");
            Assertions.assertThat(DefaultContextTest.this.response.headers).containsEntry("X-Custom", "value");
        }

        @Test
        @DisplayName("text() sends text response")
        void textSendsTextResponse() {
            DefaultContextTest.this.context.text("Hello, World!");
            Assertions.assertThat(DefaultContextTest.this.response.headers).containsEntry("Content-Type", "text/plain; charset=UTF-8");
            Assertions.assertThat(new String(DefaultContextTest.this.response.body, StandardCharsets.UTF_8)).isEqualTo("Hello, World!");
        }

        @Test
        @DisplayName("json() sends JSON response")
        void jsonSendsJsonResponse() {
            DefaultContextTest.this.context.json(new TestUser("john", 25));
            Assertions.assertThat(DefaultContextTest.this.response.headers).containsEntry("Content-Type", "application/json");
            Assertions.assertThat(new String(DefaultContextTest.this.response.body, StandardCharsets.UTF_8)).contains("\"name\":\"john\"");
        }

        @Test
        @DisplayName("send() sends raw bytes")
        void sendSendsRawBytes() {
            final byte[] data = "raw data".getBytes(StandardCharsets.UTF_8);
            DefaultContextTest.this.context.send(data);
            Assertions.assertThat(DefaultContextTest.this.response.body).isEqualTo(data);
        }

        @Test
        @DisplayName("throws ResponseCommittedException after response sent")
        void throwsAfterResponseSent() {
            DefaultContextTest.this.context.text("first");
            Assertions.assertThatThrownBy(() -> DefaultContextTest.this.context.text("second"))
                    .isInstanceOf(ResponseCommittedException.class);
        }
    }

    @Nested
    @DisplayName("State methods")
    class StateMethods {

        @Test
        @DisplayName("set() and get() manage request state")
        void setAndGetManageRequestState() {
            DefaultContextTest.this.context.set("user", "john");
            Assertions.assertThat(DefaultContextTest.this.context.get("user", String.class)).hasValue("john");
        }

        @Test
        @DisplayName("get() returns empty for missing key")
        void getReturnsEmptyForMissingKey() {
            Assertions.assertThat(DefaultContextTest.this.context.get("missing", String.class)).isEmpty();
        }

        @Test
        @DisplayName("get() returns empty for type mismatch")
        void getReturnsEmptyForTypeMismatch() {
            DefaultContextTest.this.context.set("number", 42);
            Assertions.assertThat(DefaultContextTest.this.context.get("number", String.class)).isEmpty();
        }

        @Test
        @DisplayName("getOrDefault() returns default for missing key")
        void getOrDefaultReturnsDefaultForMissingKey() {
            Assertions.assertThat(DefaultContextTest.this.context.getOrDefault("missing", String.class, "default")).isEqualTo("default");
        }
    }

    // Test implementations
    static class TestRequest implements DefaultContext.Request {
        String method = "GET";
        String path = "/";
        Map<String, String> params = new HashMap<>();
        Map<String, String> queryParams = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        byte[] body = new byte[0];

        @Override public String method() { return this.method; }
        @Override public String path() { return this.path; }
        @Override public Map<String, String> params() { return this.params; }
        @Override public void setParams(final Map<String, String> p) { this.params.putAll(p); }
        @Override public Map<String, String> queryParams() { return this.queryParams; }
        @Override public Map<String, String> headers() { return this.headers; }
        @Override public byte[] body() { return this.body; }
    }

    static class TestResponse implements DefaultContext.Response {
        int statusCode = 200;
        Map<String, String> headers = new HashMap<>();
        byte[] body;

        @Override public void status(final int code) { this.statusCode = code; }
        @Override public void header(final String name, final String value) { this.headers.put(name, value); }
        @Override public void send(final byte[] data) { this.body = data; }
    }

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
