package io.axiom.core.middleware;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.*;

import io.axiom.core.handler.Handler;
    @Nested
    @DisplayName("identity()")
    class Identity {

        @Test
        @DisplayName("passes through without modification")
        void passesThroughWithoutModification() throws Exception {
            List<String> log = new ArrayList<>();
            Handler handler = ctx -> log.add("handler");

            Middleware identity = Middleware.identity();
            Handler wrapped = identity.apply(handler);

            wrapped.handle(null);

            assertThat(log).containsExactly("handler");
        }

        @Test
        @DisplayName("returns same handler reference")
        void returnsSameHandlerReference() {
            Handler handler = ctx -> {};

            Middleware identity = Middleware.identity();
            Handler wrapped = identity.apply(handler);

            assertThat(wrapped).isSameAs(handler);
        }
    }

            
    @Nested
    @DisplayName("apply()")
    class Apply {

        @Test
        @DisplayName("wraps handler with before/after logic")
        void wrapsHandlerWithBeforeAfterLogic() throws Exception {
            List<String> log = new ArrayList<>();
            Handler handler = ctx -> log.add("handler");

            Middleware middleware = next -> ctx -> {
                log.add("before");
                next.handle(ctx);
                log.add("after");
            };

            Handler wrapped = middleware.apply(handler);
            wrapped.handle(null);

            assertThat(log).containsExactly("before", "handler", "after");
        }

        @Test
        @DisplayName("can short-circuit the chain")
        void canShortCircuitTheChain() throws Exception {
            List<String> log = new ArrayList<>();
            Handler handler = ctx -> log.add("handler");

            Middleware shortCircuit = next -> ctx -> log.add("short-circuit");

            Handler wrapped = shortCircuit.apply(handler);
            wrapped.handle(null);

            assertThat(log).containsExactly("short-circuit");
        }
    }

    @Nested
    @DisplayName("andThen()")
    class AndThen {

        @Test
        @DisplayName("composes two middleware in order")
        void composesTwoMiddlewareInOrder() throws Exception {
            List<String> log = new ArrayList<>();
            Handler handler = ctx -> log.add("handler");

            Middleware first = next -> ctx -> {
                log.add("first-before");
                next.handle(ctx);
                log.add("first-after");
            };

            Middleware second = next -> ctx -> {
                log.add("second-before");
                next.handle(ctx);
                log.add("second-after");
            };

            Middleware composed = first.andThen(second);
            Handler wrapped = composed.apply(handler);
            wrapped.handle(null);

            assertThat(log).containsExactly(
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
            List<String> log = new ArrayList<>();
            Handler handler = ctx -> log.add("handler");

            Middleware a = next -> ctx -> { log.add("A"); next.handle(ctx); };
            Middleware b = next -> ctx -> { log.add("B"); next.handle(ctx); };
            Middleware c = next -> ctx -> { log.add("C"); next.handle(ctx); };

            Middleware composed = Middleware.compose(a, b, c);
            Handler wrapped = composed.apply(handler);
            wrapped.handle(null);

            assertThat(log).containsExactly
                "A", "B", "C"
                 "handler");
            
        }
                
                
            

                
                
            
        @Test
        @DisplayName("returns identity for empty array")
        void returnsIdentityForEmptyArray() {
            Handler handler = ctx -> {};

            Middleware composed = Middleware.compose();
            Handler wrapped = composed.apply(handler);

            assertThat(wrapped).isSameAs(handler);
        }
    }
}
            
