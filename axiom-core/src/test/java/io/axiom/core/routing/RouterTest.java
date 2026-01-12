package io.axiom.core.routing;

import java.util.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;

import io.axiom.core.handler.*;

@DisplayName("Router")
class RouterTest {

    private static final Handler NOOP = ctx -> {
    };
    private Router router;

    @BeforeEach
    void setUp() {
        this.router = new Router();
    }

    @Nested
    @DisplayName("HTTP method shortcuts")
    class HttpMethodShortcuts {

        @Test
        @DisplayName("get() registers GET route")
        void getRegistersGetRoute() {
            RouterTest.this.router.get("/users", RouterTest.NOOP);
            final RouteMatch match = RouterTest.this.router.match("GET", "/users");
            Assertions.assertThat(match).isNotNull();
        }

        @Test
        @DisplayName("post() registers POST route")
        void postRegistersPostRoute() {
            RouterTest.this.router.post("/users", RouterTest.NOOP);
            final RouteMatch match = RouterTest.this.router.match("POST", "/users");
            Assertions.assertThat(match).isNotNull();
        }

        @Test
        @DisplayName("put() registers PUT route")
        void putRegistersPutRoute() {
            RouterTest.this.router.put("/users/:id", RouterTest.NOOP);
            final RouteMatch match = RouterTest.this.router.match("PUT", "/users/123");
            Assertions.assertThat(match).isNotNull();
        }

        @Test
        @DisplayName("delete() registers DELETE route")
        void deleteRegistersDeleteRoute() {
            RouterTest.this.router.delete("/users/:id", RouterTest.NOOP);
            final RouteMatch match = RouterTest.this.router.match("DELETE", "/users/123");
            Assertions.assertThat(match).isNotNull();
        }
    }

    @Nested
    @DisplayName("Route matching")
    class RouteMatching {

        @Test
        @DisplayName("matches static path")
        void matchesStaticPath() {
            RouterTest.this.router.get("/users", RouterTest.NOOP);
            final RouteMatch match = RouterTest.this.router.match("GET", "/users");
            Assertions.assertThat(match).isNotNull();
            Assertions.assertThat(match.route().path()).isEqualTo("/users");
        }

        @Test
        @DisplayName("matches parameterized path")
        void matchesParameterizedPath() {
            RouterTest.this.router.get("/users/:id", RouterTest.NOOP);
            final RouteMatch match = RouterTest.this.router.match("GET", "/users/123");
            Assertions.assertThat(match).isNotNull();
            Assertions.assertThat(match.params()).containsEntry("id", "123");
        }

        @Test
        @DisplayName("matches wildcard path")
        void matchesWildcardPath() {
            RouterTest.this.router.get("/files/*", RouterTest.NOOP);
            final RouteMatch match = RouterTest.this.router.match("GET", "/files/path/to/file.txt");
            Assertions.assertThat(match).isNotNull();
            Assertions.assertThat(match.params()).containsEntry("*", "path/to/file.txt");
        }

        @Test
        @DisplayName("returns null for non-existent route")
        void returnsNullForNonExistent() {
            RouterTest.this.router.get("/users", RouterTest.NOOP);
            final RouteMatch match = RouterTest.this.router.match("GET", "/posts");
            Assertions.assertThat(match).isNull();
        }

        @Test
        @DisplayName("returns null for wrong method")
        void returnsNullForWrongMethod() {
            RouterTest.this.router.get("/users", RouterTest.NOOP);
            final RouteMatch match = RouterTest.this.router.match("POST", "/users");
            Assertions.assertThat(match).isNull();
        }
    }

    @Nested
    @DisplayName("Route grouping")
    class RouteGrouping {

        @Test
        @DisplayName("group() prefixes all nested routes")
        void groupPrefixesRoutes() {
            RouterTest.this.router.group("/api", api -> {
                api.get("/users", RouterTest.NOOP);
                api.get("/posts", RouterTest.NOOP);
            });

            Assertions.assertThat(RouterTest.this.router.match("GET", "/api/users")).isNotNull();
            Assertions.assertThat(RouterTest.this.router.match("GET", "/api/posts")).isNotNull();
        }

        @Test
        @DisplayName("nested groups combine prefixes")
        void nestedGroupsCombinePrefixes() {
            RouterTest.this.router.group("/api", api -> {
                api.group("/v1", v1 -> {
                    v1.get("/users", RouterTest.NOOP);
                });
            });

            Assertions.assertThat(RouterTest.this.router.match("GET", "/api/v1/users")).isNotNull();
        }
    }

    @Nested
    @DisplayName("Route listing")
    class RouteListing {

        @Test
        @DisplayName("routes() returns all registered routes")
        void routesReturnsAll() {
            RouterTest.this.router.get("/users", RouterTest.NOOP);
            RouterTest.this.router.post("/users", RouterTest.NOOP);
            RouterTest.this.router.get("/posts", RouterTest.NOOP);

            final List<Route> routes = RouterTest.this.router.routes();
            Assertions.assertThat(routes).hasSize(3);
        }

        @Test
        @DisplayName("allowedMethods() returns methods for path")
        void allowedMethodsReturnsMethodsForPath() {
            RouterTest.this.router.get("/users", RouterTest.NOOP);
            RouterTest.this.router.post("/users", RouterTest.NOOP);
            RouterTest.this.router.delete("/users/:id", RouterTest.NOOP);

            final List<String> methods = RouterTest.this.router.allowedMethods("/users");
            Assertions.assertThat(methods).containsExactlyInAnyOrder("GET", "POST");
        }

        @Test
        @DisplayName("hasRoute() checks route existence")
        void hasRouteChecksExistence() {
            RouterTest.this.router.get("/users", RouterTest.NOOP);

            Assertions.assertThat(RouterTest.this.router.hasRoute("/users")).isTrue();
            Assertions.assertThat(RouterTest.this.router.hasRoute("/posts")).isFalse();
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("rejects null method")
        void rejectsNullMethod() {
            Assertions.assertThatThrownBy(() -> RouterTest.this.router.route(null, "/users", RouterTest.NOOP))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects null path")
        void rejectsNullPath() {
            Assertions.assertThatThrownBy(() -> RouterTest.this.router.route("GET", null, RouterTest.NOOP))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects null handler")
        void rejectsNullHandler() {
            Assertions.assertThatThrownBy(() -> RouterTest.this.router.route("GET", "/users", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Router merging")
    class RouterMerging {

        @Test
        @DisplayName("merge() combines routes from another router")
        void mergeCombinesRoutes() {
            final Router other = new Router();
            other.get("/posts", RouterTest.NOOP);

            RouterTest.this.router.get("/users", RouterTest.NOOP);
            RouterTest.this.router.merge(other);

            Assertions.assertThat(RouterTest.this.router.match("GET", "/users")).isNotNull();
            Assertions.assertThat(RouterTest.this.router.match("GET", "/posts")).isNotNull();
        }

        @Test
        @DisplayName("merge() with prefix prepends path")
        void mergeWithPrefixPrependsPath() {
            final Router other = new Router();
            other.get("/users", RouterTest.NOOP);

            RouterTest.this.router.merge("/api", other);

            Assertions.assertThat(RouterTest.this.router.match("GET", "/api/users")).isNotNull();
        }
    }
}
