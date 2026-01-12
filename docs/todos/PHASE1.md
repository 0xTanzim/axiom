# Phase 1 Tasks: Core Engine Implementation

**Phase:** 1
**Status:** NOT STARTED
**Target:** Core abstractions (Handler, Router, Middleware)
**Dependencies:** Phase 0 complete

---

## Overview

Phase 1 implements the fundamental engine of Axiom.
These abstractions form the foundation for all other modules.

**Constraints:**
- Zero external dependencies
- JDK stdlib only
- No I/O classes
- No HTTP-specific types

---

## Task Breakdown

### 1.1 Handler Interface

**Priority:** P0 (Critical)
**Effort:** 1 day

- [ ] Create `io.axiom.core.handler` package
- [ ] Define `Handler` functional interface
- [ ] Define `HandlerChain` for composed handlers
- [ ] Write unit tests
- [ ] Write Javadoc

```java
@FunctionalInterface
public interface Handler {
    void handle(Context c) throws Exception;
}
```

**Acceptance Criteria:**
- Functional interface with single method
- Can be used as lambda
- Exception signature allows checked exceptions

---

### 1.2 Context Interface (Abstract)

**Priority:** P0 (Critical)
**Effort:** 2 days

- [ ] Create `io.axiom.core.context` package
- [ ] Define `Context` interface (request + response)
- [ ] Define `RequestData` for immutable request view
- [ ] Define `ResponseWriter` for response output
- [ ] Define state storage API (`get`, `set`)
- [ ] Write unit tests
- [ ] Write Javadoc

```java
public interface Context {
    // Request
    String method();
    String path();
    String param(String name);
    String query(String name);
    <T> T body(Class<T> type);
    Map<String, String> headers();

    // Response
    void status(int code);
    void header(String name, String value);
    void text(String value);
    void json(Object value);
    void send(byte[] data);

    // State
    <T> Optional<T> get(String key, Class<T> type);
    void set(String key, Object value);
}
```

**Acceptance Criteria:**
- Interface only (no implementation yet)
- Clear separation of request vs response
- Type-safe state storage

---

### 1.3 Segment Sealed Hierarchy

**Priority:** P0 (Critical)
**Effort:** 1 day

- [ ] Create `io.axiom.core.routing` package
- [ ] Define `Segment` sealed interface
- [ ] Define `StaticSegment` record
- [ ] Define `ParamSegment` record
- [ ] Define `WildcardSegment` record
- [ ] Implement path parsing to segments
- [ ] Write unit tests

```java
public sealed interface Segment
    permits StaticSegment, ParamSegment, WildcardSegment {

    record StaticSegment(String value) implements Segment {}
    record ParamSegment(String name) implements Segment {}
    record WildcardSegment() implements Segment {}
}
```

**Acceptance Criteria:**
- Exhaustive switch support
- Path parsing handles all cases
- Validation on invalid paths

---

### 1.4 Route Definition

**Priority:** P0 (Critical)
**Effort:** 1 day

- [ ] Define `Route` record
- [ ] Include method, path, segments, handler
- [ ] Path normalization logic
- [ ] Conflict detection helpers
- [ ] Write unit tests

```java
public record Route(
    String method,
    String path,
    List<Segment> segments,
    Handler handler
) {
    public static Route of(String method, String path, Handler handler) {
        return new Route(method, path, parse(path), handler);
    }
}
```

**Acceptance Criteria:**
- Immutable route representation
- Path normalized at creation
- Segments pre-computed

---

### 1.5 Route Trie Implementation

**Priority:** P0 (Critical)
**Effort:** 3 days

- [ ] Create `io.axiom.core.routing.internal` package
- [ ] Define `TrieNode` class
- [ ] Implement static/param/wildcard children
- [ ] Implement route insertion
- [ ] Implement route matching
- [ ] Implement param extraction
- [ ] Conflict detection on insert
- [ ] Write comprehensive unit tests

```java
// Internal implementation
final class RouteTrie {
    void insert(Route route);
    RouteMatch match(String method, String path);
}

record RouteMatch(Route route, Map<String, String> params) {}
```

**Acceptance Criteria:**
- O(depth) matching
- Static > param > wildcard precedence
- Conflict detection works
- No regex usage

---

### 1.6 Router Public API

**Priority:** P0 (Critical)
**Effort:** 2 days

- [ ] Define `Router` class
- [ ] HTTP method registration (get, post, put, delete, etc.)
- [ ] Group support for path prefixing
- [ ] Internal trie usage
- [ ] Route listing for debugging
- [ ] Write unit tests

```java
public final class Router {
    public void get(String path, Handler handler);
    public void post(String path, Handler handler);
    public void put(String path, Handler handler);
    public void delete(String path, Handler handler);
    public void patch(String path, Handler handler);
    public void head(String path, Handler handler);
    public void options(String path, Handler handler);

    public void group(String basePath, Consumer<Router> configure);

    // Internal
    RouteMatch match(String method, String path);
}
```

**Acceptance Criteria:**
- All HTTP methods supported
- Grouping works with prefix
- Match returns handler + params

---

### 1.7 Middleware Internal Model

**Priority:** P0 (Critical)
**Effort:** 1 day

- [ ] Create `io.axiom.core.middleware` package
- [ ] Define internal `Middleware` interface
- [ ] Implement composition logic
- [ ] Write unit tests

```java
// Internal - not public API
@FunctionalInterface
interface Middleware {
    Handler apply(Handler next);
}
```

**Acceptance Criteria:**
- Pure function composition
- Chain builds correctly
- Immutable after composition

---

### 1.8 Middleware Public API

**Priority:** P0 (Critical)
**Effort:** 2 days

- [ ] Define `MiddlewareHandler` interface
- [ ] Define `Next` interface
- [ ] Implement adapter from public to internal
- [ ] Write unit tests

```java
@FunctionalInterface
public interface MiddlewareHandler {
    void handle(Context c, Next next) throws Exception;
}

@FunctionalInterface
public interface Next {
    void run() throws Exception;
}
```

**Acceptance Criteria:**
- Clean public API
- Adapts to internal model
- Next.run() continues chain

---

### 1.9 Error Types

**Priority:** P1 (High)
**Effort:** 1 day

- [ ] Create `io.axiom.core.error` package
- [ ] Define `AxiomException` base class
- [ ] Define `RouteNotFoundException`
- [ ] Define `MethodNotAllowedException`
- [ ] Define `BodyParseException`
- [ ] Define `ResponseCommittedException`
- [ ] Write Javadoc

```java
public class AxiomException extends RuntimeException {
    public AxiomException(String message) { ... }
    public AxiomException(String message, Throwable cause) { ... }
}

public final class RouteNotFoundException extends AxiomException { ... }
```

**Acceptance Criteria:**
- Clear hierarchy
- Meaningful messages
- Cause chain preserved

---

### 1.10 App Interface (Abstract)

**Priority:** P1 (High)
**Effort:** 1 day

- [ ] Create `io.axiom.core.app` package
- [ ] Define `App` interface
- [ ] Include middleware, routing, lifecycle methods
- [ ] Write Javadoc

```java
public interface App {
    void use(MiddlewareHandler middleware);
    void before(Handler hook);
    void after(Handler hook);

    void route(Router router);
    void route(String basePath, Router router);
    void route(Supplier<Router> supplier);

    void onError(ErrorHandler handler);

    void listen(int port);
    void stop();
}
```

**Acceptance Criteria:**
- Interface only
- Full API surface defined
- Ready for runtime implementation

---

### 1.11 Module Info

**Priority:** P1 (High)
**Effort:** 0.5 day

- [ ] Create `module-info.java`
- [ ] Export public packages
- [ ] Hide internal packages

```java
module io.axiom.core {
    exports io.axiom.core.handler;
    exports io.axiom.core.context;
    exports io.axiom.core.routing;
    exports io.axiom.core.middleware;
    exports io.axiom.core.lifecycle;
    exports io.axiom.core.error;
    exports io.axiom.core.app;

    // Internal not exported
}
```

**Acceptance Criteria:**
- JPMS compliant
- Internal packages hidden
- Clean export list

---

### 1.12 Integration Tests

**Priority:** P1 (High)
**Effort:** 2 days

- [ ] Router + Handler integration
- [ ] Middleware chain integration
- [ ] Error propagation tests
- [ ] Edge case coverage

**Test Scenarios:**
1. Route matching with params
2. Static/param/wildcard precedence
3. Middleware chain execution
4. Middleware short-circuit
5. Error propagation
6. Route conflicts
7. Invalid paths

**Acceptance Criteria:**
- All scenarios covered
- Edge cases tested
- 90%+ code coverage

---

## Estimated Timeline

| Task | Days | Dependencies |
|------|------|--------------|
| 1.1 Handler | 1 | - |
| 1.2 Context | 2 | 1.1 |
| 1.3 Segment | 1 | - |
| 1.4 Route | 1 | 1.3 |
| 1.5 Trie | 3 | 1.4 |
| 1.6 Router | 2 | 1.5, 1.1 |
| 1.7 Middleware Internal | 1 | 1.1 |
| 1.8 Middleware Public | 2 | 1.7, 1.2 |
| 1.9 Error Types | 1 | - |
| 1.10 App Interface | 1 | 1.6, 1.8 |
| 1.11 Module Info | 0.5 | All above |
| 1.12 Integration Tests | 2 | All above |

**Total:** ~17.5 days (3-4 weeks with buffer)

---

## Definition of Done

- [ ] All tasks completed
- [ ] All tests passing
- [ ] Code coverage > 90%
- [ ] Javadoc complete
- [ ] No external dependencies
- [ ] Module compiles standalone
- [ ] Code review passed

---

## Blockers

| Blocker | Impact | Resolution |
|---------|--------|------------|
| RFC-0007 not complete | Medium | Can proceed, refine later |
| RFC-0008 not complete | Medium | Use basic error model |

---

## Notes

- Context is interface-only in Phase 1
- Concrete implementation comes in Phase 2 (HTTP) and Phase 3 (Runtime)
- Keep everything in `core` module, split later if needed
- Prefer records and sealed types for safety

---

*Track progress by checking off tasks as completed.*
