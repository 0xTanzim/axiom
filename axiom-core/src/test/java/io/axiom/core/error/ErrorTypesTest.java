package io.axiom.core.error;

import java.util.List;

import org.junit.jupiter.api.*;
    @Nested
    @DisplayName("AxiomException")
    class AxiomExceptionTests {

        @Test
        @DisplayName("is base exception for all framework errors")
        void isBaseExceptionForAllFrameworkErrors() {
            AxiomException exception = new AxiomException("test");
            assertThat(exception).isInstanceOf(RuntimeException.class);
            assertThat(exception.getMessage()).isEqualTo("test");
        }

        @Test
        @DisplayName("supports cause chaining")
        void supportsCauseChaining() {
            Exception cause = new RuntimeException("original");
            AxiomException exception = new AxiomException("wrapped", cause);
            assertThat(exception.getCause()).isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("RouteNotFoundException")
    class RouteNotFoundExceptionTests {

        @Test
        @DisplayName("captures method and path")
        void capturesMethodAndPath() {
            var exception = new RouteNotFoundException("GET", "/users/123");
            assertThat(exception.method()).isEqualTo("GET");
            assertThat(exception.path()).isEqualTo("/users/123");
        }

        @Test
        @DisplayName("generates descriptive message")
        void generatesDescriptiveMessage() {
            var exception = new RouteNotFoundException("POST", "/api/data");
            assertThat(exception.getMessage()).contains("POST").contains("/api/data");
        }
    }

    @Nested
    @DisplayName("MethodNotAllowedException")
    class MethodNotAllowedExceptionTests {

        @Test
        @DisplayName("captures method, path, and allowed methods")
        void capturesMethodPathAndAllowedMethods() {
            var exception = new MethodNotAllowedException("DELETE", "/users", List.of("GET", "POST"));
            assertThat(exception.method()).isEqualTo("DELETE");
            assertThat(exception.path()).isEqualTo("/users");
            assertThat(exception.allowedMethods()).containsExactly("GET", "POST");
        }

        @Test
        @DisplayName("allowed methods list is immutable")
        void allowedMethodsListIsImmutable() {
            var exception = new MethodNotAllowedException("PATCH", "/data", List.of("GET"));
            assertThat(exception.allowedMethods()).isUnmodifiable();
        }
    }

    @Nested
    @DisplayName("BodyParseException")
    class BodyParseExceptionTests {

        @Test
        @DisplayName("captures message and target type")
        void capturesMessageAndTargetType() {
            var exception = new BodyParseException("Invalid JSON", String.class);
            assertThat(exception.getMessage()).isEqualTo("Invalid JSON");
            assertThat(exception.targetType()).isEqualTo(String.class);
        }

        @Test
        @DisplayName("supports cause chaining")
        void supportsCauseChaining() {
            Exception cause = new RuntimeException("parse error");
            var exception = new BodyParseException("Failed to parse", Object.class, cause);
            assertThat(exception.getCause()).isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("ResponseCommittedException")
    class ResponseCommittedExceptionTests {

        @Test
        @DisplayName("has default descriptive message")
        void hasDefaultDescriptiveMessage() {
            var exception = new ResponseCommittedException();
            assertThat(exception.getMessage()).contains("committed");
        }
    }

    @Nested
    @DisplayName("Exception hierarchy")
    class ExceptionHierarchy {

        @Test
        @DisplayName("all framework exceptions extend AxiomException")
        void allFrameworkExceptionsExtendAxiomException() {
            assertThat(new RouteNotFoundException("GET", "/")).isInstanceOf(AxiomException.class);
            assertThat(new MethodNotAllowedException("POST", "/", List.of("GET"))).isInstanceOf(AxiomException.class);
            assertThat(new BodyParseException("error", String.class)).isInstanceOf(AxiomException.class);
            assertThat(new ResponseCommittedException()).isInstanceOf(AxiomException.class);
        }
    }
}
