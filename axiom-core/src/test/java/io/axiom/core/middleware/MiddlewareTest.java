package io.axiom.core.middleware;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;

import io.axiom.core.handler.Handler;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Middleware")
class MiddlewareTest {

    @Nested
    @DisplayName("identity()")
    class Identity {

        @Test
        @DisplayName("passes through without modification")
        void passesThroughWithoutModification() throws Exception {
            final List<String> log = new ArrayList<>();
            final Handler handler = ctx -> log.add("handler");

            final Middleware identity = Middleware.identity();
            final Handler wrapped = identity.apply(handler);

            wrapped.handle(null);

            Assertions.assertThat(log).containsExactly("handler");
        }

        @Test
        @DisplayName("returns same handler reference")
        void returnsSameHandlerReference() {
            final Handler handler = ctx -> {};

            final Middleware identity = Middleware.identity();
            final Handler wrapped = identity.apply(handler);

            Assertions.assertThat(wrapped).isSameAs(handler);
        }
    }

    @Nested
    @DisplayName("apply()")
    class Apply {

        @Test
        @DisplayName("wraps handler with before/after logic")
        void wrapsHandlerWithBeforeAfterLogic() throws Exception {
            final List<String> log = new ArrayList<>();
            final Handler handler = ctx -> log.add("handler");

            final Middleware middleware = next -> ctx -> {
                log.add("before");
                next.handle(ctx);
                log.add("after");
            };

            final Handler wrapped = middleware.apply(handler);
            wrapped.handle(null);

            Assertions.assertThat(log).containsExactly("before", "handler", "after");
        }

        @Test
        @DisplayName("can short-circuit the chain")
        void canShortCircuitTheChain() throws Exception {
            final List<String> log = new ArrayList<>();
            final Handler handler = ctx -> log.add("handler");

            final Middleware shortCircuit = next -> ctx -> log.add("short-circuit");

            final Handler wrapped = shortCircuit.apply(handler);
            wrapped.handle(null);

            Assertions.assertThat(log).containsExactly("short-circuit");
        }
    }

    @Nested
    @DisplayName("andThen()")
    class AndThen {

        @Test
        @DisplayName("composes two middleware in order")
        void composesTwoMiddlewareInOrder() throws Exception {
            final List<String> log = new ArrayList<>();
            final Handler handler = ctx -> log.add("handler");

            final Middleware first = next -> ctx -> {
                log.add("first-before");
                next.handle(ctx);
                log.add("first-after");
            };

            final Middleware second = next -> ctx -> {
                log.add("second-before");
                next.handle(ctx);
                log.add("second-after");
            };

            final Middleware composed = first.andThen(second);
            final Handler wrapped = composed.apply(handler);
            wrapped.handle(null);

            Assertions.assertThat(log).containsExactly(
                    "first-before",
                    "second-before",
                    "handler",
                    "second-after",
                    "first-after");
        }
    }

    @Nested
    @DisplayName("compose()")
    class Compose {

        @Test
        @DisplayName("composes multiple middleware in array order")
        void composesMultipleMiddlewareInArrayOrder() throws Exception {
            final List<String> log = new ArrayList<>();
            final Handler handler = ctx -> log.add("handler");

            final Middleware a = next -> ctx -> { log.add("A"); next.handle(ctx); };
            final Middleware b = next -> ctx -> { log.add("B"); next.handle(ctx); };
            final Middleware c = next -> ctx -> { log.add("C"); next.handle(ctx); };

            final Middleware composed = Middleware.compose(a, b, c);
            final Handler wrapped = composed.apply(handler);
            wrapped.handle(null);

            Assertions.assertThat(log).containsExactly("A", "B", "C", "handler");
        }

        @Test
        @DisplayName("returns identity for empty array")
        void returnsIdentityForEmptyArray() {
            final Handler handler = ctx -> {};

            final Middleware composed = Middleware.compose();
            final Handler wrapped = composed.apply(handler);

            Assertions.assertThat(wrapped).isSameAs(handler);
        }
    }
}
