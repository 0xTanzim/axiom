# Axiom Framework — Progress Report

**Date:** January 12, 2026
**Status:** Phase 1 Complete ✅

---

## What We've Done (Phase 1)

Phase 1 established the **core routing infrastructure** for Axiom — a modern, DX-first Java web framework targeting Java 25 LTS.

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

### Test Coverage

- **79 tests** passing
- Tests cover routing, middleware, error types
- JUnit 5 + AssertJ for assertions

### Architecture Principles Applied

- ✅ Zero reflection in hot paths
- ✅ Explicit composition over magic
- ✅ Functional core (Middleware as `Handler → Handler`)
- ✅ Sealed interfaces for segment types
- ✅ Trie-based O(depth) routing
- ✅ No annotations required

---

## What's Next (Phase 2)

Based on the RFCs, the next phase focuses on:

### 1. Context Implementation (RFC-0001)

The `Context` interface needs concrete implementation with:

- Request access (`path()`, `method()`, `param()`, `query()`, `header()`, `body()`)
- Response methods (`status()`, `text()`, `json()`, `html()`)
- State management (`get()`, `set()`)

### 2. Application Entry Point (RFC-0002)

Create the `App` class that:

- Mounts routers via `route()` method
- Applies global middleware via `use()`
- Starts server via `listen(port)`
- Supports both middleware and before/after hooks

### 3. Middleware Integration (RFC-0004, RFC-0005)

Complete the middleware pipeline:

- Public `MiddlewareHandler` with `(Context, Next)` signature
- Adapt user-facing API to internal `Middleware` type
- Support both middleware style and before/after hooks

### 4. Runtime Adapters

Create runtime implementations:

- JDK HttpServer adapter (development)
- Netty adapter (production, optional)

---

## RFC Summary

| RFC | Title | Status |
|-----|-------|--------|
| RFC-0001 | Core Design & Handler API | Partial (Handler done, Context pending) |
| RFC-0002 | Routing & App Composition | Partial (Router done, App pending) |
| RFC-0003 | Routing Matcher Algorithm | ✅ Complete (RouteTrie implemented) |
| RFC-0004 | Middleware Pipeline | Partial (internal done, public API pending) |
| RFC-0005 | DX Philosophy & Style Freedom | Design only |
| RFC-0006 | Build Tool & Kotlin Strategy | Design only |

---

## Technical Stack

- **Java Version:** 25 LTS (Temurin)
- **Build System:** Maven 3.9.12
- **Testing:** JUnit 5.11.0 + AssertJ 3.26.0
- **Module System:** JPMS enabled

---

## Immediate Next Steps

1. **Implement `Context`** — concrete request/response handling
2. **Create `App` class** — application entry point with router mounting
3. **Add JDK HttpServer adapter** — minimal runtime for development
4. **Complete public middleware API** — user-facing `(Context, Next)` signature

The goal is to achieve the target DX from RFC-0001:

```java
Router router = new Router();
router.get("/health", c -> c.text("OK"));
router.get("/users/:id", c -> c.json(userService.find(c.param("id"))));

App app = new App();
app.use(auth());
app.route(router);
app.listen(8080);
```
