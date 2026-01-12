# Axiom Framework — Progress Report

**Date:** January 12, 2026
**Status:** Phase 1 + Runtime Complete ✅

---

## What We've Done

### Phase 1: Core Routing Infrastructure

Phase 1 established the **core routing infrastructure** for Axiom — a modern, DX-first Java web framework targeting Java 25 LTS.

### Phase 2: Runtime Adapter + Server SPI

Implemented **auto-discovering server runtime** via ServiceLoader SPI.

| Package | Component | Description |
|---------|-----------|-------------|
| `io.axiom.core.server` | `Server` | HTTP server abstraction interface |
| `io.axiom.core.server` | `ServerFactory` | SPI for server runtime discovery |
| `io.axiom.server` | `JdkServer` | JDK HttpServer with virtual threads |
| `io.axiom.server` | `JdkServerFactory` | ServiceLoader-discovered factory |

**Key Features:**
- ✅ Zero-config server startup: `app.listen(8080)` auto-discovers runtime
- ✅ Virtual threads via `Executors.newVirtualThreadPerTaskExecutor()`
- ✅ Java 25 compatible (JEP 491 fixes synchronized pinning)
- ✅ JPMS `provides`/`uses` + META-INF/services fallback
- ✅ SPI allows multiple runtime implementations (JDK, Netty, etc.)

### Completed Components

| Package | Component | Description |
|---------|-----------|-------------|
| `io.axiom.core.handler` | `Handler` | Core functional interface for request handling |
| `io.axiom.core.handler` | `Context` | Request/response context interface |
| `io.axiom.core.routing` | `Segment` | Sealed interface for path segment types |
| `io.axiom.core.routing` | `StaticSegment` | Static path segment (e.g., `users`) |
| `io.axiom.core.routing` | `ParamSegment` | Parameter segment (e.g., `:id`) |
| `io.axiom.core.routing` | `WildcardSegment` | Wildcard segment (`*`) |
| `io.axiom.core.routing` | `PathParser` | Path parsing and normalization |
| `io.axiom.core.routing` | `Route` | Route definition with method, path, handler |
| `io.axiom.core.routing` | `RouteMatch` | Match result with extracted params |
| `io.axiom.core.routing` | `Router` | Route registration and matching API |
| `io.axiom.core.routing.internal` | `RouteTrie` | Trie-based O(depth) route matching |
| `io.axiom.core.routing.internal` | `RouteConflictException` | Route registration conflict error |
| `io.axiom.core.middleware` | `Middleware` | Functional middleware composition |
| `io.axiom.core.error` | `AxiomException` | Base framework exception |
| `io.axiom.core.error` | `RouteNotFoundException` | 404 error type |
| `io.axiom.core.error` | `MethodNotAllowedException` | 405 error type |
| `io.axiom.core.error` | `BodyParseException` | Body parsing error |
| `io.axiom.core.error` | `ResponseCommittedException` | Response already sent error |
| `io.axiom.core.app` | `App` | Application interface |
| `io.axiom.core.app` | `DefaultApp` | Default implementation with middleware chain |
| `io.axiom.core.app` | `Axiom` | Factory with server auto-discovery |
| `io.axiom.core.server` | `Server` | HTTP server abstraction |
| `io.axiom.core.server` | `ServerFactory` | SPI for runtime discovery |
| `io.axiom.server` | `JdkServer` | JDK HttpServer + virtual threads |
| `io.axiom.server` | `JdkServerFactory` | Default server factory |

### Test Coverage

- **144 tests** passing (135 core + 9 integration)
- Tests cover routing, middleware, error types, app composition, JSON codec
- Integration tests verify real HTTP requests through JdkServer
- JUnit 5 + AssertJ for assertions

### Architecture Principles Applied

- ✅ Zero reflection in hot paths
- ✅ Explicit composition over magic
- ✅ Functional core (Middleware as `Handler → Handler`)
- ✅ Sealed interfaces for segment types
- ✅ Trie-based O(depth) routing
- ✅ No annotations required
- ✅ SPI-based server discovery (core doesn't depend on runtime)
- ✅ Virtual threads for massive concurrency

---

## User DX

```java
// Create app (no runtime mention needed!)
App app = Axiom.create();

// Register routes
Router router = new Router();
router.get("/hello", ctx -> ctx.text("Hello!"));
router.get("/users/:id", ctx -> ctx.json(userService.find(ctx.param("id"))));

// Add middleware
app.use((ctx, next) -> {
    log.info("Request: {} {}", ctx.method(), ctx.path());
    next.run();
});

// Mount routes and start
app.route(router);
app.listen(8080);  // Auto-discovers jdk-httpserver runtime
```

---

## RFC Summary

| RFC | Title | Status |
|-----|-------|--------|
| RFC-0001 | Core Design & Handler API | ✅ Complete (Handler, Context, DefaultContext) |
| RFC-0002 | Routing & App Composition | ✅ Complete (Router, App, DefaultApp, Axiom) |
| RFC-0003 | Routing Matcher Algorithm | ✅ Complete (RouteTrie implemented) |
| RFC-0004 | Middleware Pipeline | ✅ Complete (dual style: explicit next + ctx.next()) |
| RFC-0005 | DX Philosophy & Style Freedom | Design only |
| RFC-0006 | JSON Codec Strategy | ✅ Complete (JsonCodec, JacksonCodec) |
| RFC-0007 | Lifecycle Management | ❌ Not written |
| RFC-0008 | Error Handling Architecture | ❌ Not written |
| RFC-0009 | Runtime Adapter Contract | ❌ Not written |
| RFC-0010 | Testing Utilities API | ❌ Not written |

---

## Technical Stack

- **Java Version:** 25 LTS (Temurin)
- **Build System:** Maven 3.9.12
- **Testing:** JUnit 5.11.0 + AssertJ 3.26.0
- **Module System:** JPMS enabled

---

## Immediate Next Steps

1. ✅ ~~Implement `Context`~~ — **DONE** (DefaultContext with JSON support)
2. ✅ ~~Create `App` class~~ — **DONE** (App, DefaultApp, Axiom factory)
3. ✅ ~~Complete public middleware API~~ — **DONE** (dual style supported)
4. **Add JDK HttpServer runtime adapter** — **NEXT TARGET** (make framework runnable)
5. **Write RFC-0009** — Runtime Adapter Contract
6. **Create example application** — Demonstrate complete usage

**Current Blocker:** No runtime adapter = framework can't actually serve HTTP

The target DX from RFC-0001 is **90% achievable**:

```java
Router router = new Router();
router.get("/health", ctx -> ctx.text("OK"));
router.get("/users/:id", ctx -> ctx.json(userService.find(ctx.param("id"))));

App app = Axiom.create();
app.use(auth());
app.route(router);
app.listen(8080); // ⚠️ NEEDS RUNTIME ADAPTER
```
