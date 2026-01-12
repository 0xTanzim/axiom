package io.axiom.core.routing;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;

import io.axiom.core.handler.*;

@DisplayName("Route")
class RouteTest {

    private static final Handler NOOP = ctx -> {
    };

    @Nested
    @DisplayName("Route.of()")
    class Creation {

        @Test
        @DisplayName("creates route with method, path, handler")
        void createsRoute() {
            final Route route = Route.of("GET", "/users", RouteTest.NOOP);
            Assertions.assertThat(route.method()).isEqualTo("GET");
            Assertions.assertThat(route.path()).isEqualTo("/users");
            Assertions.assertThat(route.handler()).isNotNull();
        }

        @Test
        @DisplayName("normalizes method to uppercase")
        void normalizesMethod() {
            final Route route = Route.of("get", "/users", RouteTest.NOOP);
            Assertions.assertThat(route.method()).isEqualTo("GET");
        }
    }

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("get() creates GET route")
        void getCreatesGetRoute() {
            final Route route = Route.get("/users", RouteTest.NOOP);
            Assertions.assertThat(route.method()).isEqualTo("GET");
        }

        @Test
        @DisplayName("post() creates POST route")
        void postCreatesPostRoute() {
            final Route route = Route.post("/users", RouteTest.NOOP);
            Assertions.assertThat(route.method()).isEqualTo("POST");
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("rejects null method")
        void rejectsNullMethod() {
            Assertions.assertThatThrownBy(() -> Route.of(null, "/users", RouteTest.NOOP))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects invalid HTTP method")
        void rejectsInvalidMethod() {
            Assertions.assertThatThrownBy(() -> Route.of("INVALID", "/users", RouteTest.NOOP))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Segment analysis")
    class SegmentAnalysis {

        @Test
        @DisplayName("hasParams() returns true for parameterized routes")
        void hasParamsWorks() {
            final Route withParams = Route.of("GET", "/users/:id", RouteTest.NOOP);
            final Route noParams = Route.of("GET", "/users", RouteTest.NOOP);
            Assertions.assertThat(withParams.hasParams()).isTrue();
            Assertions.assertThat(noParams.hasParams()).isFalse();
        }

        @Test
        @DisplayName("hasWildcard() returns true for wildcard routes")
        void hasWildcardWorks() {
            final Route withWildcard = Route.of("GET", "/files/*", RouteTest.NOOP);
            final Route noWildcard = Route.of("GET", "/files", RouteTest.NOOP);
            Assertions.assertThat(withWildcard.hasWildcard()).isTrue();
            Assertions.assertThat(noWildcard.hasWildcard()).isFalse();
        }
    }
}
