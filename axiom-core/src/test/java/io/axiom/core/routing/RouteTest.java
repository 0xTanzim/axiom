package io.axiom.core.routing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.axiom.core.handler.Handler;

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
            Route route = Route.of("GET", "/users", NOOP);
            assertThat(route.method()).isEqualTo("GET");
            assertThat(route.path()).isEqualTo("/users");
            assertThat(route.handler()).isNotNull();
        }

        @Test
        @DisplayName("normalizes method to uppercase")
        void normalizesMethod() {
            Route route = Route.of("get", "/users", NOOP);
            assertThat(route.method()).isEqualTo("GET");
        }
    }

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("get() creates GET route")
        void getCreatesGetRoute() {
            Route route = Route.get("/users", NOOP);
            assertThat(route.method()).isEqualTo("GET");
        }

        @Test
        @DisplayName("post() creates POST route")
        void postCreatesPostRoute() {
            Route route = Route.post("/users", NOOP);
            assertThat(route.method()).isEqualTo("POST");
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("rejects null method")
        void rejectsNullMethod() {
            assertThatThrownBy(() -> Route.of(null, "/users", NOOP))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects invalid HTTP method")
        void rejectsInvalidMethod() {
            assertThatThrownBy(() -> Route.of("INVALID", "/users", NOOP))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Segment analysis")
    class SegmentAnalysis {

        @Test
        @DisplayName("hasParams() returns true for parameterized routes")
        void hasParamsWorks() {
            Route withParams = Route.of("GET", "/users/:id", NOOP);
            Route noParams = Route.of("GET", "/users", NOOP);
            assertThat(withParams.hasParams()).isTrue();
            assertThat(noParams.hasParams()).isFalse();
        }

        @Test
        @DisplayName("hasWildcard() returns true for wildcard routes")
        void hasWildcardWorks() {
            Route withWildcard = Route.of("GET", "/files/*", NOOP);
            Route noWildcard = Route.of("GET", "/files", NOOP);
            assertThat(withWildcard.hasWildcard()).isTrue();
            assertThat(noWildcard.hasWildcard()).isFalse();
        }
    }
}
