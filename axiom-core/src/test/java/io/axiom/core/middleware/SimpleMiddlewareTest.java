package io.axiom.core.middleware;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;

import io.axiom.core.context.*;
import io.axiom.core.handler.*;
import io.axiom.core.json.*;

@DisplayName("SimpleMiddleware")
class SimpleMiddlewareTest {

    @Nested
    @DisplayName("Context.next()")
    class ContextNext {

        @Test
        @DisplayName("calls next handler when ctx.next() invoked")
        void callsNextHandlerWhenCtxNextInvoked() throws Exception {
            final AtomicBoolean nextCalled = new AtomicBoolean(false);

            final SimpleMiddleware middleware = ctx -> {
                ctx.next();
            };

            final Handler nextHandler = ctx -> nextCalled.set(true);
            final MiddlewareFunction adapted = MiddlewareAdapter.adapt(middleware);
            final Handler composed = adapted.apply(nextHandler);

            composed.handle(SimpleMiddlewareTest.this.createContext());

            Assertions.assertThat(nextCalled.get()).isTrue();
        }

        @Test
        @DisplayName("skips next handler when ctx.next() not called")
        void skipsNextHandlerWhenCtxNextNotCalled() throws Exception {
            final AtomicBoolean nextCalled = new AtomicBoolean(false);

            final SimpleMiddleware middleware = ctx -> {
                // Don't call ctx.next()
            };

            final Handler nextHandler = ctx -> nextCalled.set(true);
            final MiddlewareFunction adapted = MiddlewareAdapter.adapt(middleware);
            final Handler composed = adapted.apply(nextHandler);

            composed.handle(SimpleMiddlewareTest.this.createContext());

            Assertions.assertThat(nextCalled.get()).isFalse();
        }

        @Test
        @DisplayName("executes before/after code around next")
        void executesBeforeAfterCodeAroundNext() throws Exception {
            final List<String> order = new ArrayList<>();

            final SimpleMiddleware middleware = ctx -> {
                order.add("before");
                ctx.next();
                order.add("after");
            };

            final Handler nextHandler = ctx -> order.add("handler");
            final MiddlewareFunction adapted = MiddlewareAdapter.adapt(middleware);
            final Handler composed = adapted.apply(nextHandler);

            composed.handle(SimpleMiddlewareTest.this.createContext());

            Assertions.assertThat(order).containsExactly("before", "handler", "after");
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("propagates exceptions from next handler")
        void propagatesExceptionsFromNextHandler() {
            final SimpleMiddleware middleware = ctx -> {
                ctx.next();
            };

            final Handler nextHandler = ctx -> {
                throw new RuntimeException("Handler error");
            };

            final MiddlewareFunction adapted = MiddlewareAdapter.adapt(middleware);
            final Handler composed = adapted.apply(nextHandler);

            Assertions.assertThatThrownBy(() -> composed.handle(SimpleMiddlewareTest.this.createContext()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Handler error");
        }

        @Test
        @DisplayName("catches exceptions with try/catch")
        void catchesExceptionsWithTryCatch() throws Exception {
            final AtomicReference<String> caughtMessage = new AtomicReference<>();

            final SimpleMiddleware middleware = ctx -> {
                try {
                    ctx.next();
                } catch (final Exception e) {
                    caughtMessage.set(e.getMessage());
                }
            };

            final Handler nextHandler = ctx -> {
                throw new RuntimeException("Caught error");
            };

            final MiddlewareFunction adapted = MiddlewareAdapter.adapt(middleware);
            final Handler composed = adapted.apply(nextHandler);

            composed.handle(SimpleMiddlewareTest.this.createContext());

            Assertions.assertThat(caughtMessage.get()).isEqualTo("Caught error");
        }

        @Test
        @DisplayName("throws UnsupportedOperationException outside middleware context")
        void throwsUnsupportedOperationExceptionOutsideMiddlewareContext() {
            final Context ctx = SimpleMiddlewareTest.this.createContextWithoutNext();

            Assertions.assertThatThrownBy(ctx::next)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("only available in middleware context");
        }
    }

    @Nested
    @DisplayName("Adapter")
    class Adapter {

        @Test
        @DisplayName("rejects null middleware")
        void rejectsNullMiddleware() {
            Assertions.assertThatThrownBy(() -> MiddlewareAdapter.adapt((SimpleMiddleware) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("forPath only applies to matching paths")
        void forPathOnlyAppliesToMatchingPaths() throws Exception {
            final AtomicInteger middlewareCount = new AtomicInteger(0);

            final MiddlewareFunction conditionalMiddleware = MiddlewareAdapter.forPath("/api",
                    next -> ctx -> {
                        middlewareCount.incrementAndGet();
                        next.handle(ctx);
                    });

            final Handler finalHandler = ctx -> {};

            // Matching path
            final Handler composed = conditionalMiddleware.apply(finalHandler);
            composed.handle(SimpleMiddlewareTest.this.createContext("GET", "/api/users"));
            Assertions.assertThat(middlewareCount.get()).isEqualTo(1);

            // Non-matching path
            middlewareCount.set(0);
            composed.handle(SimpleMiddlewareTest.this.createContext("GET", "/health"));
            Assertions.assertThat(middlewareCount.get()).isEqualTo(0);
        }

        @Test
        @DisplayName("afterAlways runs even on exception")
        void afterAlwaysRunsEvenOnException() throws Exception {
            final AtomicBoolean afterRan = new AtomicBoolean(false);

            final MiddlewareFunction afterAlways = MiddlewareAdapter.afterAlways(ctx -> afterRan.set(true));

            final Handler failingHandler = ctx -> {
                throw new RuntimeException("Fail");
            };

            final Handler composed = afterAlways.apply(failingHandler);

            Assertions.assertThatThrownBy(() -> composed.handle(SimpleMiddlewareTest.this.createContext()))
                    .isInstanceOf(RuntimeException.class);

            Assertions.assertThat(afterRan.get()).isTrue();
        }
    }

    // ========== Test Utilities ==========

    private DefaultContext createContext() {
        return this.createContext("GET", "/test");
    }

    private DefaultContext createContext(final String method, final String path) {
        return new DefaultContext(
                new TestRequest(method, path),
                new TestResponse(),
                new JacksonCodec());
    }

    private Context createContextWithoutNext() {
        // DefaultContext without setNext() called
        return new DefaultContext(
                new TestRequest("GET", "/test"),
                new TestResponse(),
                new JacksonCodec());
    }

    static class TestRequest implements DefaultContext.Request {
        final String method;
        final String path;
        Map<String, String> params = new HashMap<>();

        TestRequest(final String method, final String path) {
            this.method = method;
            this.path = path;
        }

        @Override public String method() { return this.method; }
        @Override public String path() { return this.path; }
        @Override public Map<String, String> params() { return this.params; }
        @Override public void setParams(final Map<String, String> p) { this.params.putAll(p); }
        @Override public Map<String, String> queryParams() { return Map.of(); }
        @Override public Map<String, String> headers() { return Map.of(); }
        @Override public byte[] body() { return new byte[0]; }
    }

    static class TestResponse implements DefaultContext.Response {
        int status = 200;
        Map<String, String> headers = new HashMap<>();
        byte[] body;

        @Override public void status(final int code) { this.status = code; }
        @Override public void header(final String name, final String value) { this.headers.put(name, value); }
        @Override public void send(final byte[] data) { this.body = data; }
    }
}
