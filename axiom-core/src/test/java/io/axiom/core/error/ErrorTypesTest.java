package io.axiom.core.error;

import java.util.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;

@DisplayName("Error Types")
class ErrorTypesTest {

    @Nested
    @DisplayName("AxiomException")
    class AxiomExceptionTests {

        @Test
        @DisplayName("is base exception for all framework errors")
        void isBaseExceptionForAllFrameworkErrors() {
            final AxiomException exception = new AxiomException("test");
            Assertions.assertThat(exception).isInstanceOf(RuntimeException.class);
            Assertions.assertThat(exception.getMessage()).isEqualTo("test");
        }

        @Test
        @DisplayName("supports cause chaining")
        void supportsCauseChaining() {
            final Exception cause = new RuntimeException("original");
            final AxiomException exception = new AxiomException("wrapped", cause);
            Assertions.assertThat(exception.getCause()).isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("RouteNotFoundException")
    class RouteNotFoundExceptionTests {

        @Test
        @DisplayName("captures method and path")
        void capturesMethodAndPath() {
            final var exception = new RouteNotFoundException("GET", "/users/123");
            Assertions.assertThat(exception.method()).isEqualTo("GET");
            Assertions.assertThat(exception.path()).isEqualTo("/users/123");
        }

        @Test
        @DisplayName("generates descriptive message")
        void generatesDescriptiveMessage() {
            final var exception = new RouteNotFoundException("POST", "/api/data");
            Assertions.assertThat(exception.getMessage()).contains("POST").contains("/api/data");
        }
    }

    @Nested
    @DisplayName("MethodNotAllowedException")
    class MethodNotAllowedExceptionTests {

        @Test
        @DisplayName("captures method, path, and allowed methods")
        void capturesMethodPathAndAllowedMethods() {
            final var exception = new MethodNotAllowedException("DELETE", "/users", List.of("GET", "POST"));
            Assertions.assertThat(exception.method()).isEqualTo("DELETE");
            Assertions.assertThat(exception.path()).isEqualTo("/users");
            Assertions.assertThat(exception.allowedMethods()).containsExactly("GET", "POST");
        }

        @Test
        @DisplayName("allowed methods list is immutable")
        void allowedMethodsListIsImmutable() {
            final var exception = new MethodNotAllowedException("PATCH", "/data", List.of("GET"));
            Assertions.assertThat(exception.allowedMethods()).isUnmodifiable();
        }
    }

    @Nested
    @DisplayName("BodyParseException")
    class BodyParseExceptionTests {

        @Test
        @DisplayName("captures message and target type")
        void capturesMessageAndTargetType() {
            final var exception = new BodyParseException("Invalid JSON", String.class);
            Assertions.assertThat(exception.getMessage()).isEqualTo("Invalid JSON");
            Assertions.assertThat(exception.targetType()).isEqualTo(String.class);
        }

        @Test
        @DisplayName("supports cause chaining")
        void supportsCauseChaining() {
            final Exception cause = new RuntimeException("parse error");
            final var exception = new BodyParseException("Failed to parse", Object.class, cause);
            Assertions.assertThat(exception.getCause()).isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("ResponseCommittedException")
    class ResponseCommittedExceptionTests {

        @Test
        @DisplayName("has default descriptive message")
        void hasDefaultDescriptiveMessage() {
            final var exception = new ResponseCommittedException();
            Assertions.assertThat(exception.getMessage()).contains("committed");
        }
    }

    @Nested
    @DisplayName("Exception hierarchy")
    class ExceptionHierarchy {

        @Test
        @DisplayName("all framework exceptions extend AxiomException")
        void allFrameworkExceptionsExtendAxiomException() {
            Assertions.assertThat(new RouteNotFoundException("GET", "/")).isInstanceOf(AxiomException.class);
            Assertions.assertThat(new MethodNotAllowedException("POST", "/", List.of("GET"))).isInstanceOf(AxiomException.class);
            Assertions.assertThat(new BodyParseException("error", String.class)).isInstanceOf(AxiomException.class);
            Assertions.assertThat(new ResponseCommittedException()).isInstanceOf(AxiomException.class);
        }
    }
}
