package io.axiom.core.routing;

import java.util.List;

import org.junit.jupiter.api.*;

import io.axiom.core.handler.Handler;

@DisplayName("Router")
class RouterTest {

    private static final Handler NOOP = ctx -> {
    };
    private Router router;

    @BeforeEach
    void setUp() {
        router = new Router();
    }

    @Nested
    @DisplayName("HTTP method shortcuts")
    class HttpMethodShortcuts {

        @Test
        @DisplayName("get() registers GET route")
        void getRegistersGetRoute() {
            router.get("/users", NOOP);
            RouteMatch match = router.match("GET", "/users");
            assertThat(match).isNotNull();
        }

        @Test
        @DisplayName("post() registers POST route")
        void postRegistersPostRoute() {
            router.post("/users", NOOP);
            RouteMatch match = router.match("POST", "/users");
            assertThat(match).isNotNull();
        }

        @Test
        @DisplayName("put() registers PUT route")
        void putRegistersPutRoute() {
            router.put("/users/:id", NOOP);
            RouteMatch match = router.match("PUT", "/users/123");
            assertThat(match).isNotNull();
        }

        @Test
        @DisplayName("delete() registers DELETE route")
        void deleteRegistersDeleteRoute() {
            router.delete("/users/:id", NOOP);
            RouteMatch match = router.match("DELETE", "/users/123");
            assertThat(match).isNotNull();
        }
    }

    @Nested
    @DisplayName("Route matching")
    class RouteMatching {

        @Test
        @DisplayName("matches static path")
        void matchesStaticPath() {
            router.get("/users", NOOP);
            RouteMatch match = router.match("GET", "/users");
            assertThat(match).isNotNull();
            assertThat(match.route().path()).isEqualTo("/users");
        }

        @Test
        @DisplayName("matches parameterized path")
        void matchesParameterizedPath() {
            router.get("/users/:id", NOOP);
            RouteMatch match = router.match("GET", "/users/123");
            assertThat(match).isNotNull();
            assertThat(match.params()).containsEntry("id", "123");
        }

        @Test
        @DisplayName("matches wildcard path")
        void matchesWildcardPath() {
            router.get("/files/*", NOOP);
            RouteMatch match = router.match("GET", "/files/path/to/file.txt");
            assertThat(match).isNotNull();
            assertThat(match.params()).containsEntry("*", "path/to/file.txt");
        }

        @Test
        @DisplayName("returns null for non-existent route")
        void returnsNullForNonExistent() {
            router.get("/users", NOOP);
            RouteMatch match = router.match("GET", "/posts");
            assertThat(match).isNull();
        }

        @Test
        @DisplayName("returns null for wrong method")
        void returnsNullForWrongMethod() {
            router.get("/users", NOOP);
            RouteMatch match = router.match("POST", "/users");
            assertThat(match).isNull();
        }
    }

    @Nested
    @DisplayName("Route grouping")
    class RouteGrouping {

        @Test
        @DisplayName("group() prefixes all nested routes")
        void groupPrefixesRoutes() {
            router.group("/api", api -> {
                api.get("/users", NOOP);
                api.get("/posts", NOOP);
            });

            assertThat(router.match("GET", "/api/users")).isNotNull();
            assertThat(router.match("GET", "/api/posts")).isNotNull();
        }

        @Test
        @DisplayName("nested groups combine prefixes")
        void nestedGroupsCombinePrefixes() {
            router.group("/api", api -> {
                api.group("/v1", v1 -> {
                    v1.get("/users", NOOP);
                });
            });

            assertThat(router.match("GET", "/api/v1/users")).isNotNull();
        }
    }

    @Nested
    @DisplayName("Route listing")
    class RouteListing {

        @Test
        @DisplayName("routes() returns all registered routes")
        void routesReturnsAll() {
            router.get("/users", NOOP);
            router.post("/users", NOOP);
            router.get("/posts", NOOP);

            List<Route> routes = router.routes();
            assertThat(routes).hasSize(3);
        }

        @Test
        @DisplayName("allowedMethods() returns methods for path")
        void allowedMethodsReturnsMethodsForPath() {
            router.get("/users", NOOP);
            router.post("/users", NOOP);
            router.delete("/users/:id", NOOP);

            List<String> methods = router.allowedMethods("/users");
            assertThat(methods).containsExactlyInAnyOrder("GET", "POST");
        }

        @Test
        @DisplayName("hasRoute() checks route existence")
        void hasRouteChecksExistence() {
            router.get("/users", NOOP);

            assertThat(router.hasRoute("/users")).isTrue();
            assertThat(router.hasRoute("/posts")).isFalse();
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("rejects null method")
        void rejectsNullMethod() {
            assertThatThrownBy(() -> router.route(null, "/users", NOOP))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects null path")
        void rejectsNullPath() {
            assertThatThrownBy(() -> router.route("GET", null, NOOP))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects null handler")
        void rejectsNullHandler() {
            assertThatThrownBy(() -> router.route("GET", "/users", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Router merging")
    class RouterMerging {

        @Test
        @DisplayName("merge() combines routes from another router")
        void mergeCombinesRoutes() {
            Router other = new Router();
            other.get("/posts", NOOP);

            router.get("/users", NOOP);
            router.merge(other);

            assertThat(router.match("GET", "/users")).isNotNull();
            assertThat(router.match("GET", "/posts")).isNotNull();
        }

        @Test
        @DisplayName("merge() with prefix prepends path")
        void mergeWithPrefixPrependsPath() {
            Router other = new Router();
            other.get("/users", NOOP);

            router.merge("/api", other);

            assertThat(router.match("GET", "/api/users")).isNotNull();
        }
    }
}
