package io.axiom.core.routing.internal;

import java.util.List;

import org.junit.jupiter.api.*;

import io.axiom.core.handler.Handler;
import io.axiom.core.routing.Route;
import io.axiom.core.routing.RouteMatch;

@DisplayName("RouteTrie")
class RouteTrieTest {

    private static final Handler NOOP = ctx -> {
    };
    private RouteTrie trie;

    @BeforeEach
    void setUp() {
        trie = new RouteTrie();
    }

    @Nested
    @DisplayName("insert()")
    class Insert {

        @Test
        @DisplayName("inserts static route")
        void insertsStaticRoute() {
            Route route = Route.get("/users", NOOP);
            trie.insert(route);

            RouteMatch match = trie.match("GET", "/users");
            assertThat(match).isNotNull();
        }

        @Test
        @DisplayName("inserts parameterized route")
        void insertsParameterizedRoute() {
            Route route = Route.get("/users/:id", NOOP);
            trie.insert(route);

            RouteMatch match = trie.match("GET", "/users/123");
            assertThat(match).isNotNull();
            assertThat(match.params()).containsEntry("id", "123");
        }

        @Test
        @DisplayName("inserts wildcard route")
        void insertsWildcardRoute() {
            Route route = Route.get("/files/*", NOOP);
            trie.insert(route);

            RouteMatch match = trie.match("GET", "/files/path/to/file.txt");
            assertThat(match).isNotNull();
            assertThat(match.params()).containsEntry("*", "path/to/file.txt");
        }

        @Test
        @DisplayName("throws on duplicate route")
        void throwsOnDuplicateRoute() {
            Route route1 = Route.get("/users", NOOP);
            Route route2 = Route.get("/users", NOOP);

            trie.insert(route1);
            assertThatThrownBy(() -> trie.insert(route2))
                    .isInstanceOf(RouteConflictException.class);
        }

        @Test
        @DisplayName("throws on parameter name conflict")
        void throwsOnParameterNameConflict() {
            Route route1 = Route.get("/users/:id", NOOP);
            Route route2 = Route.get("/users/:userId/posts", NOOP);

            trie.insert(route1);
            assertThatThrownBy(() -> trie.insert(route2))
                    .isInstanceOf(RouteConflictException.class);
        }
    }

    @Nested
    @DisplayName("match()")
    class Match {

        @Test
        @DisplayName("matches root path")
        void matchesRootPath() {
            Route route = Route.get("/", NOOP);
            trie.insert(route);

            RouteMatch match = trie.match("GET", "/");
            assertThat(match).isNotNull();
        }

        @Test
        @DisplayName("prioritizes static over parameter")
        void prioritizesStaticOverParameter() {
            Route staticRoute = Route.get("/users/admin", NOOP);
            Route paramRoute = Route.get("/users/:id", NOOP);

            trie.insert(staticRoute);
            trie.insert(paramRoute);

            RouteMatch match = trie.match("GET", "/users/admin");
            assertThat(match.route().path()).isEqualTo("/users/admin");
        }

        @Test
        @DisplayName("prioritizes parameter over wildcard")
        void prioritizesParameterOverWildcard() {
            Route paramRoute = Route.get("/files/:name", NOOP);
            Route wildcardRoute = Route.get("/files/*", NOOP);

            trie.insert(paramRoute);
            trie.insert(wildcardRoute);

            RouteMatch match = trie.match("GET", "/files/document.pdf");
            assertThat(match.route().path()).isEqualTo("/files/:name");
        }

        @Test
        @DisplayName("returns null for non-matching path")
        void returnsNullForNonMatchingPath() {
            Route route = Route.get("/users", NOOP);
            trie.insert(route);

            RouteMatch match = trie.match("GET", "/posts");
            assertThat(match).isNull();
        }

        @Test
        @DisplayName("returns null for non-matching method")
        void returnsNullForNonMatchingMethod() {
            Route route = Route.get("/users", NOOP);
            trie.insert(route);

            RouteMatch match = trie.match("POST", "/users");
            assertThat(match).isNull();
        }

        @Test
        @DisplayName("normalizes method to uppercase")
        void normalizesMethodToUppercase() {
            Route route = Route.get("/users", NOOP);
            trie.insert(route);

            RouteMatch match = trie.match("get", "/users");
            assertThat(match).isNotNull();
        }
    }

    @Nested
    @DisplayName("routes()")
    class Routes {

        @Test
        @DisplayName("returns empty list when no routes")
        void returnsEmptyWhenNoRoutes() {
            List<Route> routes = trie.routes();
            assertThat(routes).isEmpty();
        }

        @Test
        @DisplayName("returns all registered routes")
        void returnsAllRegisteredRoutes() {
            trie.insert(Route.get("/users", NOOP));
            trie.insert(Route.post("/users", NOOP));
            trie.insert(Route.get("/posts", NOOP));

            List<Route> routes = trie.routes();
            assertThat(routes).hasSize(3);
        }
    }

    @Nested
    @DisplayName("allowedMethods()")
    class AllowedMethods {

        @Test
        @DisplayName("returns methods for matching path")
        void returnsMethodsForMatchingPath() {
            trie.insert(Route.get("/users", NOOP));
            trie.insert(Route.post("/users", NOOP));
            trie.insert(Route.delete("/users", NOOP));

            List<String> methods = trie.allowedMethods("/users");
            assertThat(methods).containsExactlyInAnyOrder("GET", "POST", "DELETE");
        }

        @Test
        @DisplayName("returns empty list for non-matching path")
        void returnsEmptyForNonMatchingPath() {
            trie.insert(Route.get("/users", NOOP));

            List<String> methods = trie.allowedMethods("/posts");
            assertThat(methods).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasPath()")
    class HasPath {

        @Test
        @DisplayName("returns true for existing path")
        void returnsTrueForExistingPath() {
            trie.insert(Route.get("/users", NOOP));

            assertThat(trie.hasPath("/users")).isTrue();
        }

        @Test
        @DisplayName("returns false for non-existing path")
        void returnsFalseForNonExistingPath() {
            trie.insert(Route.get("/users", NOOP));

            assertThat(trie.hasPath("/posts")).isFalse();
        }
    }
}
