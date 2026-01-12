package io.axiom.core.app;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;

import io.axiom.core.context.*;
import io.axiom.core.handler.*;
import io.axiom.core.json.*;
import io.axiom.core.routing.*;

@DisplayName("DefaultApp")
class DefaultAppTest {

    private DefaultApp app;

    @BeforeEach
    void setUp() {
        this.app = new DefaultApp();
    }

    @Nested
    @DisplayName("Middleware - Style 1: Explicit Next")
    class MiddlewareExplicitNext {

        @Test
        @DisplayName("executes middleware in order")
        void executesMiddlewareInOrder() throws Exception {
            final List<String> order = new ArrayList<>();

            DefaultAppTest.this.app.use((ctx, next) -> {
                order.add("m1-before");
                next.run();
                order.add("m1-after");
            });

            DefaultAppTest.this.app.use((ctx, next) -> {
                order.add("m2-before");
                next.run();
                order.add("m2-after");
            });

            final Router router = new Router();
            router.get("/test", ctx -> order.add("handler"));
            DefaultAppTest.this.app.route(router);

            final Handler handler = DefaultAppTest.this.app.buildHandler();
            handler.handle(DefaultAppTest.this.createContext("GET", "/test").ctx());

            Assertions.assertThat(order).containsExactly(
                    "m1-before", "m2-before", "handler", "m2-after", "m1-after");
        }

        @Test
        @DisplayName("short-circuits when next not called")
        void shortCircuitsWhenNextNotCalled() throws Exception {
            final AtomicBoolean handlerCalled = new AtomicBoolean(false);

            DefaultAppTest.this.app.use((ctx, next) -> {
                ctx.status(401);
                ctx.text("Unauthorized");
            });

            final Router router = new Router();
            router.get("/test", ctx -> handlerCalled.set(true));
            DefaultAppTest.this.app.route(router);

            final TestableContext testCtx = DefaultAppTest.this.createContext("GET", "/test");
            DefaultAppTest.this.app.buildHandler().handle(testCtx.ctx());

            Assertions.assertThat(handlerCalled.get()).isFalse();
            Assertions.assertThat(testCtx.getStatus()).isEqualTo(401);
        }
    }

    @Nested
    @DisplayName("Middleware - Style 2: Context-Embedded Next")
    class MiddlewareContextNext {

        @Test
        @DisplayName("executes middleware with ctx.next()")
        void executesMiddlewareWithCtxNext() throws Exception {
            final List<String> order = new ArrayList<>();

            DefaultAppTest.this.app.use(ctx -> {
                order.add("m1-before");
                ctx.next();
                order.add("m1-after");
            });

            DefaultAppTest.this.app.use(ctx -> {
                order.add("m2-before");
                ctx.next();
                order.add("m2-after");
            });

            final Router router = new Router();
            router.get("/test", ctx -> order.add("handler"));
            DefaultAppTest.this.app.route(router);

            final Handler handler = DefaultAppTest.this.app.buildHandler();
            handler.handle(DefaultAppTest.this.createContext("GET", "/test").ctx());

            Assertions.assertThat(order).containsExactly(
                    "m1-before", "m2-before", "handler", "m2-after", "m1-after");
        }

        @Test
        @DisplayName("short-circuits when ctx.next() not called")
        void shortCircuitsWhenCtxNextNotCalled() throws Exception {
            final AtomicBoolean handlerCalled = new AtomicBoolean(false);

            DefaultAppTest.this.app.use(ctx -> {
                ctx.status(403);
                ctx.text("Forbidden");
            });

            final Router router = new Router();
            router.get("/test", ctx -> handlerCalled.set(true));
            DefaultAppTest.this.app.route(router);

            final TestableContext testCtx = DefaultAppTest.this.createContext("GET", "/test");
            DefaultAppTest.this.app.buildHandler().handle(testCtx.ctx());

            Assertions.assertThat(handlerCalled.get()).isFalse();
            Assertions.assertThat(testCtx.getStatus()).isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("Middleware - Mixed Styles")
    class MiddlewareMixedStyles {

        @Test
        @DisplayName("supports both styles in same app")
        void supportsBothStylesInSameApp() throws Exception {
            final List<String> order = new ArrayList<>();

            DefaultAppTest.this.app.use((ctx, next) -> {
                order.add("explicit-next");
                next.run();
            });

            DefaultAppTest.this.app.use(ctx -> {
                order.add("ctx-next");
                ctx.next();
            });

            final Router router = new Router();
            router.get("/test", ctx -> order.add("handler"));
            DefaultAppTest.this.app.route(router);

            DefaultAppTest.this.app.buildHandler().handle(DefaultAppTest.this.createContext("GET", "/test").ctx());

            Assertions.assertThat(order).containsExactly("explicit-next", "ctx-next", "handler");
        }
    }

    @Nested
    @DisplayName("Before/After Hooks")
    class BeforeAfterHooks {

        @Test
        @DisplayName("before hook runs before handler")
        void beforeHookRunsBeforeHandler() throws Exception {
            final List<String> order = new ArrayList<>();

            DefaultAppTest.this.app.before(ctx -> order.add("before"));

            final Router router = new Router();
            router.get("/test", ctx -> order.add("handler"));
            DefaultAppTest.this.app.route(router);

            DefaultAppTest.this.app.buildHandler().handle(DefaultAppTest.this.createContext("GET", "/test").ctx());

            Assertions.assertThat(order).containsExactly("before", "handler");
        }

        @Test
        @DisplayName("after hook runs after handler")
        void afterHookRunsAfterHandler() throws Exception {
            final List<String> order = new ArrayList<>();

            DefaultAppTest.this.app.after(ctx -> order.add("after"));

            final Router router = new Router();
            router.get("/test", ctx -> order.add("handler"));
            DefaultAppTest.this.app.route(router);

            DefaultAppTest.this.app.buildHandler().handle(DefaultAppTest.this.createContext("GET", "/test").ctx());

            Assertions.assertThat(order).containsExactly("handler", "after");
        }
    }

    @Nested
    @DisplayName("Routing")
    class Routing {

        @Test
        @DisplayName("matches routes correctly")
        void matchesRoutesCorrectly() throws Exception {
            final AtomicReference<String> capturedId = new AtomicReference<>();

            final Router router = new Router();
            router.get("/users/:id", ctx -> capturedId.set(ctx.param("id")));
            DefaultAppTest.this.app.route(router);

            DefaultAppTest.this.app.buildHandler().handle(DefaultAppTest.this.createContext("GET", "/users/123").ctx());

            Assertions.assertThat(capturedId.get()).isEqualTo("123");
        }

        @Test
        @DisplayName("merges routers with prefix")
        void mergesRoutersWithPrefix() throws Exception {
            final AtomicBoolean called = new AtomicBoolean(false);

            final Router apiRouter = new Router();
            apiRouter.get("/health", ctx -> called.set(true));

            DefaultAppTest.this.app.route("/api/v1", apiRouter);

            DefaultAppTest.this.app.buildHandler().handle(DefaultAppTest.this.createContext("GET", "/api/v1/health").ctx());

            Assertions.assertThat(called.get()).isTrue();
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("handles RouteNotFoundException with 404")
        void handlesRouteNotFoundWith404() throws Exception {
            final TestableContext testCtx = DefaultAppTest.this.createContext("GET", "/nonexistent");
            DefaultAppTest.this.app.buildHandler().handle(testCtx.ctx());

            Assertions.assertThat(testCtx.getStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("handles MethodNotAllowedException with 405")
        void handlesMethodNotAllowedWith405() throws Exception {
            final Router router = new Router();
            router.get("/test", ctx -> ctx.text("OK"));
            DefaultAppTest.this.app.route(router);

            final TestableContext testCtx = DefaultAppTest.this.createContext("POST", "/test");
            DefaultAppTest.this.app.buildHandler().handle(testCtx.ctx());

            Assertions.assertThat(testCtx.getStatus()).isEqualTo(405);
            Assertions.assertThat(testCtx.getHeaders().get("Allow")).contains("GET");
        }

        @Test
        @DisplayName("custom error handler is used")
        void customErrorHandlerIsUsed() throws Exception {
            final AtomicBoolean customHandlerCalled = new AtomicBoolean(false);

            DefaultAppTest.this.app.onError((ctx, e) -> {
                customHandlerCalled.set(true);
                ctx.status(418);
                ctx.text("Custom error");
            });

            final TestableContext testCtx = DefaultAppTest.this.createContext("GET", "/nonexistent");
            DefaultAppTest.this.app.buildHandler().handle(testCtx.ctx());

            Assertions.assertThat(customHandlerCalled.get()).isTrue();
            Assertions.assertThat(testCtx.getStatus()).isEqualTo(418);
        }

        @Test
        @DisplayName("catches handler exceptions")
        void catchesHandlerExceptions() throws Exception {
            final Router router = new Router();
            router.get("/error", ctx -> {
                throw new RuntimeException("Test error");
            });
            DefaultAppTest.this.app.route(router);

            final TestableContext testCtx = DefaultAppTest.this.createContext("GET", "/error");
            DefaultAppTest.this.app.buildHandler().handle(testCtx.ctx());

            Assertions.assertThat(testCtx.getStatus()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("rejects null middleware")
        void rejectsNullMiddleware() {
            Assertions.assertThatThrownBy(() -> DefaultAppTest.this.app.use((io.axiom.core.middleware.MiddlewareHandler) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects null router")
        void rejectsNullRouter() {
            Assertions.assertThatThrownBy(() -> DefaultAppTest.this.app.route((Router) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects null error handler")
        void rejectsNullErrorHandler() {
            Assertions.assertThatThrownBy(() -> DefaultAppTest.this.app.onError(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("handles empty middleware chain")
        void handlesEmptyMiddlewareChain() throws Exception {
            final Router router = new Router();
            router.get("/test", ctx -> ctx.text("OK"));
            DefaultAppTest.this.app.route(router);

            final TestableContext testCtx = DefaultAppTest.this.createContext("GET", "/test");
            DefaultAppTest.this.app.buildHandler().handle(testCtx.ctx());

            Assertions.assertThat(testCtx.getBodyAsString()).isEqualTo("OK");
        }
    }

    @Nested
    @DisplayName("Factory")
    class Factory {

        @Test
        @DisplayName("Axiom.create() returns App")
        void axiomCreateReturnsApp() {
            final App createdApp = Axiom.create();
            Assertions.assertThat(createdApp).isInstanceOf(DefaultApp.class);
        }

        @Test
        @DisplayName("Axiom.createDefault() returns DefaultApp")
        void axiomCreateDefaultReturnsDefaultApp() {
            final DefaultApp createdApp = Axiom.createDefault();
            Assertions.assertThat(createdApp).isNotNull();
        }
    }

    // ========== Test Utilities ==========

    private TestableContext createContext(final String method, final String path) {
        return new TestableContext(method, path);
    }

    static class TestableContext {
        private final DefaultContext context;
        private final TestResponse response;

        TestableContext(final String method, final String path) {
            final TestRequest request = new TestRequest(method, path);
            this.response = new TestResponse();
            this.context = new DefaultContext(request, this.response, new JacksonCodec());
        }

        DefaultContext ctx() {
            return this.context;
        }

        int getStatus() {
            return this.response.status;
        }

        Map<String, String> getHeaders() {
            return this.response.headers;
        }

        String getBodyAsString() {
            return this.response.body != null ? new String(this.response.body) : null;
        }

        static class TestRequest implements DefaultContext.Request {
            final String method;
            final String path;
            Map<String, String> params = new HashMap<>();
            Map<String, String> queryParams = new HashMap<>();
            Map<String, String> headers = new HashMap<>();

            TestRequest(final String method, final String path) {
                this.method = method;
                this.path = path;
            }

            @Override
            public String method() {
                return this.method;
            }

            @Override
            public String path() {
                return this.path;
            }

            @Override
            public Map<String, String> params() {
                return this.params;
            }

            @Override
            public void setParams(final Map<String, String> p) {
                this.params.putAll(p);
            }

            @Override
            public Map<String, String> queryParams() {
                return this.queryParams;
            }

            @Override
            public Map<String, String> headers() {
                return this.headers;
            }

            @Override
            public byte[] body() {
                return new byte[0];
            }
        }

        static class TestResponse implements DefaultContext.Response {
            int status = 200;
            Map<String, String> headers = new HashMap<>();
            byte[] body;

            @Override
            public void status(final int code) {
                this.status = code;
            }

            @Override
            public void header(final String name, final String value) {
                this.headers.put(name, value);
            }

            @Override
            public void send(final byte[] data) {
                this.body = data;
            }
        }
    }
}
