package io.axiom.core.routing.internal;

import java.util.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;

import io.axiom.core.handler.*;
import io.axiom.core.routing.*;

@DisplayName("RouteTrie")
class RouteTrieTest {

    private static final Handler NOOP = ctx -> {
    };
    private RouteTrie trie;

    @BeforeEach
    void setUp() {
        this.trie = new RouteTrie();
    }

    @Nested
    @DisplayName("insert()")
    class Insert {

        @Test
        @DisplayName("inserts static route")
        void insertsStaticRoute() {
            final Route route = Route.get("/users", RouteTrieTest.NOOP);
            RouteTrieTest.this.trie.insert(route);

            final RouteMatch match = RouteTrieTest.this.trie.match("GET", "/users");
            Assertions.assertThat(match).isNotNull();
        }

        @Test
        @DisplayName("inserts parameterized route")
        void insertsParameterizedRoute() {
            final Route route = Route.get("/users/:id", RouteTrieTest.NOOP);
            RouteTrieTest.this.trie.insert(route);

            final RouteMatch match = RouteTrieTest.this.trie.match("GET", "/users/123");
            Assertions.assertThat(match).isNotNull();
            Assertions.assertThat(match.params()).containsEntry("id", "123");
        }

        @Test
        @DisplayName("inserts wildcard route")
        void insertsWildcardRoute() {
            final Route route = Route.get("/files/*", RouteTrieTest.NOOP);
            RouteTrieTest.this.trie.insert(route);

            final RouteMatch match = RouteTrieTest.this.trie.match("GET", "/files/path/to/file.txt");
            Assertions.assertThat(match).isNotNull();
            Assertions.assertThat(match.params()).containsEntry("*", "path/to/file.txt");
        }

        @Test
        @DisplayName("throws on duplicate route")
        void throwsOnDuplicateRoute() {
            final Route route1 = Route.get("/users", RouteTrieTest.NOOP);
            final Route route2 = Route.get("/users", RouteTrieTest.NOOP);

            RouteTrieTest.this.trie.insert(route1);
            Assertions.assertThatThrownBy(() -> RouteTrieTest.this.trie.insert(route2))
                    .isInstanceOf(RouteConflictException.class);
        }

        @Test
        @DisplayName("throws on parameter name conflict")
        void throwsOnParameterNameConflict() {
            final Route route1 = Route.get("/users/:id", RouteTrieTest.NOOP);
            final Route route2 = Route.get("/users/:userId/posts", RouteTrieTest.NOOP);

            RouteTrieTest.this.trie.insert(route1);
            Assertions.assertThatThrownBy(() -> RouteTrieTest.this.trie.insert(route2))
                    .isInstanceOf(RouteConflictException.class);
        }
    }

    @Nested
    @DisplayName("match()")
    class Match {

        @Test
        @DisplayName("matches root path")
        void matchesRootPath() {
            final Route route = Route.get("/", RouteTrieTest.NOOP);
            RouteTrieTest.this.trie.insert(route);

            final RouteMatch match = RouteTrieTest.this.trie.match("GET", "/");
            Assertions.assertThat(match).isNotNull();
        }

        @Test
        @DisplayName("prioritizes static over parameter")
        void prioritizesStaticOverParameter() {
            final Route staticRoute = Route.get("/users/admin", RouteTrieTest.NOOP);
            final Route paramRoute = Route.get("/users/:id", RouteTrieTest.NOOP);

            RouteTrieTest.this.trie.insert(staticRoute);
            RouteTrieTest.this.trie.insert(paramRoute);

            final RouteMatch match = RouteTrieTest.this.trie.match("GET", "/users/admin");
            Assertions.assertThat(match.route().path()).isEqualTo("/users/admin");
        }

        @Test
        @DisplayName("prioritizes parameter over wildcard")
        void prioritizesParameterOverWildcard() {
            final Route paramRoute = Route.get("/files/:name", RouteTrieTest.NOOP);
            final Route wildcardRoute = Route.get("/files/*", RouteTrieTest.NOOP);

            RouteTrieTest.this.trie.insert(paramRoute);
            RouteTrieTest.this.trie.insert(wildcardRoute);

            final RouteMatch match = RouteTrieTest.this.trie.match("GET", "/files/document.pdf");
            Assertions.assertThat(match.route().path()).isEqualTo("/files/:name");
        }

        @Test
        @DisplayName("returns null for non-matching path")
        void returnsNullForNonMatchingPath() {
            final Route route = Route.get("/users", RouteTrieTest.NOOP);
            RouteTrieTest.this.trie.insert(route);

            final RouteMatch match = RouteTrieTest.this.trie.match("GET", "/posts");
            Assertions.assertThat(match).isNull();
        }

        @Test
        @DisplayName("returns null for non-matching method")
        void returnsNullForNonMatchingMethod() {
            final Route route = Route.get("/users", RouteTrieTest.NOOP);
            RouteTrieTest.this.trie.insert(route);

            final RouteMatch match = RouteTrieTest.this.trie.match("POST", "/users");
            Assertions.assertThat(match).isNull();
        }

        @Test
        @DisplayName("normalizes method to uppercase")
        void normalizesMethodToUppercase() {
            final Route route = Route.get("/users", RouteTrieTest.NOOP);
            RouteTrieTest.this.trie.insert(route);

            final RouteMatch match = RouteTrieTest.this.trie.match("get", "/users");
            Assertions.assertThat(match).isNotNull();
        }
    }

    @Nested
    @DisplayName("routes()")
    class Routes {

        @Test
        @DisplayName("returns empty list when no routes")
        void returnsEmptyWhenNoRoutes() {
            final List<Route> routes = RouteTrieTest.this.trie.routes();
            Assertions.assertThat(routes).isEmpty();
        }

        @Test
        @DisplayName("returns all registered routes")
        void returnsAllRegisteredRoutes() {
            RouteTrieTest.this.trie.insert(Route.get("/users", RouteTrieTest.NOOP));
            RouteTrieTest.this.trie.insert(Route.post("/users", RouteTrieTest.NOOP));
            RouteTrieTest.this.trie.insert(Route.get("/posts", RouteTrieTest.NOOP));

            final List<Route> routes = RouteTrieTest.this.trie.routes();
            Assertions.assertThat(routes).hasSize(3);
        }
    }

    @Nested
    @DisplayName("allowedMethods()")
    class AllowedMethods {

        @Test
        @DisplayName("returns methods for matching path")
        void returnsMethodsForMatchingPath() {
            RouteTrieTest.this.trie.insert(Route.get("/users", RouteTrieTest.NOOP));
            RouteTrieTest.this.trie.insert(Route.post("/users", RouteTrieTest.NOOP));
            RouteTrieTest.this.trie.insert(Route.delete("/users", RouteTrieTest.NOOP));

            final List<String> methods = RouteTrieTest.this.trie.allowedMethods("/users");
            Assertions.assertThat(methods).containsExactlyInAnyOrder("GET", "POST", "DELETE");
        }

        @Test
        @DisplayName("returns empty list for non-matching path")
        void returnsEmptyForNonMatchingPath() {
            RouteTrieTest.this.trie.insert(Route.get("/users", RouteTrieTest.NOOP));

            final List<String> methods = RouteTrieTest.this.trie.allowedMethods("/posts");
            Assertions.assertThat(methods).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasPath()")
    class HasPath {

        @Test
        @DisplayName("returns true for existing path")
        void returnsTrueForExistingPath() {
            RouteTrieTest.this.trie.insert(Route.get("/users", RouteTrieTest.NOOP));

            Assertions.assertThat(RouteTrieTest.this.trie.hasPath("/users")).isTrue();
        }

        @Test
        @DisplayName("returns false for non-existing path")
        void returnsFalseForNonExistingPath() {
            RouteTrieTest.this.trie.insert(Route.get("/users", RouteTrieTest.NOOP));

            Assertions.assertThat(RouteTrieTest.this.trie.hasPath("/posts")).isFalse();
        }
    }
}
