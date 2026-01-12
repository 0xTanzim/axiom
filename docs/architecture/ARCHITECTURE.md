# Axiom Framework Architecture

**Version:** 0.1.0-draft
**Status:** Architecture Phase
**Target Java Version:** 21+ (minimum), 25 LTS (primary)
**Last Updated:** 2026-01-12

---

## Executive Summary

Axiom is a DX-first, functional Java web framework designed for modern JVM development.
It delivers Express/Hono-style developer experience without reflection, annotations, or magic.

**Core Principles:**
- Developer experience above all
- Explicit over implicit
- Functional composition
- Zero reflection in hot paths
- Runtime adapter flexibility
- Build-tool agnostic

---

## 1. Module Architecture

### 1.1 Tier Model

```
┌─────────────────────────────────────────────────────────────────┐
│                         TIER 4: EXTRAS                          │
│  axiom-json-jackson │ axiom-json-gson │ axiom-validation │ ...  │
├─────────────────────────────────────────────────────────────────┤
│                      TIER 3: RUNTIME ADAPTERS                   │
│    axiom-runtime-jdk │ axiom-runtime-netty │ axiom-runtime-*    │
├─────────────────────────────────────────────────────────────────┤
│                       TIER 2: axiom-http                        │
│         HTTP abstractions without server binding                │
├─────────────────────────────────────────────────────────────────┤
│                       TIER 1: axiom-core                        │
│              Pure abstractions, zero I/O, ~15KB                 │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 Dependency Rules (STRICT)

| Module | Can Depend On | Cannot Depend On |
|--------|--------------|------------------|
| axiom-core | JDK stdlib only | Any other module |
| axiom-http | axiom-core | Runtime adapters |
| axiom-runtime-* | axiom-core, axiom-http | Other runtimes |
| axiom-extras | axiom-core, axiom-http | Runtime adapters |

**Rule:** Dependencies flow DOWN the tiers, never UP or SIDEWAYS.

---

## 2. Core Module (axiom-core)

### 2.1 Package Structure

```
io.axiom.core/
├── handler/
│   ├── Handler.java              # @FunctionalInterface void handle(Context)
│   └── HandlerChain.java         # Composed handler sequence
│
├── context/
│   ├── Context.java              # Primary DX interface
│   ├── RequestData.java          # Immutable request view
│   └── ResponseWriter.java       # Response write contract
│
├── routing/
│   ├── Router.java               # Route registration & composition
│   ├── Route.java                # Single route definition
│   ├── RouteMatch.java           # Match result with params
│   ├── Segment.java              # Sealed: Static | Param | Wildcard
│   └── RouteTrie.java            # Trie-based matcher (internal)
│
├── middleware/
│   ├── Middleware.java           # Internal: Handler -> Handler
│   ├── MiddlewareHandler.java    # Public: (Context, Next) -> void
│   └── Next.java                 # Continuation interface
│
├── lifecycle/
│   ├── Lifecycle.java            # Lifecycle phase definitions
│   ├── LifecycleHook.java        # Hook contract
│   └── LifecycleAware.java       # Marker for lifecycle participants
│
├── error/
│   ├── AxiomException.java       # Base framework exception
│   ├── RouteNotFoundException.java
│   ├── BodyParseException.java
│   └── LifecycleException.java
│
└── app/
    └── App.java                  # Application composition interface
```

### 2.2 Core Contracts

```java
// Handler - the fundamental unit
@FunctionalInterface
public interface Handler {
    void handle(Context c) throws Exception;
}

// Context - primary DX surface
public interface Context {
    // Request (immutable)
    String method();
    String path();
    String param(String name);
    String query(String name);
    <T> T body(Class<T> type);
    Map<String, String> headers();

    // Response (mutable)
    void status(int code);
    void header(String name, String value);
    void text(String value);
    void json(Object value);
    void send(byte[] data);

    // State
    <T> Optional<T> get(String key, Class<T> type);
    void set(String key, Object value);
}

// Middleware - public API
@FunctionalInterface
public interface MiddlewareHandler {
    void handle(Context c, Next next) throws Exception;
}

@FunctionalInterface
public interface Next {
    void run() throws Exception;
}
```

### 2.3 Routing Segments (Sealed)

```java
public sealed interface Segment
    permits StaticSegment, ParamSegment, WildcardSegment {

    record StaticSegment(String value) implements Segment {}
    record ParamSegment(String name) implements Segment {}
    record WildcardSegment() implements Segment {}
}
```

---

## 3. HTTP Module (axiom-http)

### 3.1 Package Structure

```
io.axiom.http/
├── HttpMethod.java           # Enum: GET, POST, PUT, DELETE, etc.
├── HttpStatus.java           # Status codes with reason phrases
├── Headers.java              # Header name constants
├── ContentType.java          # MIME type handling
│
├── body/
│   ├── BodyParser.java       # Body parsing contract
│   ├── BodyCodec.java        # Encode/decode interface
│   └── JsonCodec.java        # JSON codec interface
│
└── context/
    └── HttpContext.java      # HTTP-specific context extension
```

### 3.2 HTTP-Specific Types

```java
public enum HttpMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE;

    public static HttpMethod of(String method) {
        return valueOf(method.toUpperCase());
    }
}

public record HttpStatus(int code, String reason) {
    public static final HttpStatus OK = new HttpStatus(200, "OK");
    public static final HttpStatus CREATED = new HttpStatus(201, "Created");
    public static final HttpStatus NOT_FOUND = new HttpStatus(404, "Not Found");
    // ... more
}
```

---

## 4. Runtime Adapters (axiom-runtime-*)

### 4.1 Server Contract

```java
public interface Server {
    void start(ServerConfig config) throws LifecycleException;
    void stop() throws LifecycleException;

    int port();
    boolean isRunning();

    void onRequest(Handler handler);
}

public record ServerConfig(
    int port,
    int backlog,
    Duration readTimeout,
    Duration writeTimeout,
    boolean virtualThreads
) {
    public static ServerConfig defaults() {
        return new ServerConfig(8080, 128,
            Duration.ofSeconds(30), Duration.ofSeconds(30), true);
    }
}
```

### 4.2 Adapter Responsibilities

Each runtime adapter MUST:
1. Implement `Server` interface
2. Create `Context` from native request
3. Execute handler on virtual thread (if enabled)
4. Propagate exceptions to error handler
5. Write response to native output
6. Handle lifecycle correctly

### 4.3 Planned Adapters

| Adapter | Priority | Use Case |
|---------|----------|----------|
| axiom-runtime-jdk | P0 | Default, JDK HttpServer |
| axiom-runtime-netty | P1 | High performance |
| axiom-runtime-undertow | P2 | Alternative |
| axiom-runtime-helidon | P3 | Future |

---

## 5. Application Composition

### 5.1 App Interface

```java
public interface App {
    // Middleware
    void use(MiddlewareHandler middleware);
    void before(Handler hook);
    void after(Handler hook);

    // Routing
    void route(Router router);
    void route(String basePath, Router router);
    void route(Supplier<Router> supplier);
    void route(String basePath, Supplier<Router> supplier);

    // Error handling
    void onError(ErrorHandler handler);

    // Lifecycle
    void listen(int port);
    void listen(ServerConfig config);
    void stop();
}

@FunctionalInterface
public interface ErrorHandler {
    void handle(Context c, Throwable error);
}
```

### 5.2 Execution Pipeline

```
Request arrives
    │
    ▼
┌─────────────────┐
│  Before Hooks   │ ── (can short-circuit)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Middleware    │ ── (onion model)
│   Chain Entry   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Route Match    │ ── 404 if not found
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│     Handler     │ ── User code
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Middleware    │ ── Unwind
│   Chain Exit    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   After Hooks   │
└────────┬────────┘
         │
         ▼
Response sent
```

---

## 6. Routing Architecture

### 6.1 Trie Structure

```
GET
 └─ users
     ├─ :id          → Handler A
     │   └─ posts
     │       └─ :postId → Handler B
     ├─ me           → Handler C (static wins)
     └─ *            → Handler D (catch-all)

POST
 └─ users           → Handler E
```

### 6.2 Match Precedence

1. **Static** segments match first
2. **Param** (`:name`) matches if no static
3. **Wildcard** (`*`) matches last, consumes rest

### 6.3 Path Normalization

At registration time:
- Remove trailing slashes (except root)
- Collapse double slashes
- Validate segment format
- Detect conflicts

---

## 7. Lifecycle Management

### 7.1 Lifecycle Phases

```
INIT → STARTING → STARTED → STOPPING → STOPPED
                     │
                     └──── ERROR ────┘
```

### 7.2 Hook Types

```java
public sealed interface LifecycleHook
    permits OnStart, OnReady, OnShutdown, OnError {

    record OnStart(Runnable action) implements LifecycleHook {}
    record OnReady(Runnable action) implements LifecycleHook {}
    record OnShutdown(Runnable action) implements LifecycleHook {}
    record OnError(Consumer<Throwable> action) implements LifecycleHook {}
}
```

### 7.3 Shutdown Behavior

1. Stop accepting new requests
2. Wait for in-flight requests (timeout)
3. Run shutdown hooks in reverse order
4. Close server resources
5. Exit cleanly

---

## 8. Error Handling

### 8.1 Exception Hierarchy

```
AxiomException (RuntimeException)
├── RouteNotFoundException
├── MethodNotAllowedException
├── BodyParseException
├── ResponseCommittedException
├── LifecycleException
│   ├── StartupException
│   └── ShutdownException
└── ConfigurationException
```

### 8.2 Error Propagation

1. Handler throws → caught by middleware chain
2. Middleware throws → caught by error handler
3. Error handler throws → framework logs, sends 500
4. Lifecycle error → fail fast with clear message

---

## 9. Threading Model

### 9.1 Virtual Threads (Default)

```java
// Request handling
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

// Each request on its own virtual thread
executor.submit(() -> handler.handle(context));
```

### 9.2 ScopedValue for Request Context

```java
// Framework internal
static final ScopedValue<RequestContext> REQUEST = ScopedValue.newInstance();

// Usage in handler
ScopedValue.where(REQUEST, ctx).run(() -> {
    handler.handle(context);
});
```

### 9.3 Concurrency Guarantees

- Context is NOT thread-safe (request-scoped)
- Router is immutable after startup
- Middleware chain is immutable
- State map is thread-local to request

---

## 10. Performance Targets

### 10.1 Benchmarks

| Operation | Target | Notes |
|-----------|--------|-------|
| Route match | < 100ns | Trie lookup |
| Middleware (3-layer) | < 500ns | Pre-composed |
| Full request (hello) | < 10μs | End-to-end |
| Throughput | 100K+ req/s | Simple handler |

### 10.2 Memory Budget

| Component | Allocation |
|-----------|------------|
| Core startup | Zero after init |
| Per request | Context + Params |
| Large body | Streaming mode |

### 10.3 Hot Path Rules

1. No reflection
2. No allocation in routing
3. No regex per request
4. Lazy body parsing
5. Pre-composed middleware

---

## 11. Module Dependencies

### 11.1 External Dependencies

| Module | Dependencies |
|--------|-------------|
| axiom-core | JDK only |
| axiom-http | JDK only |
| axiom-runtime-jdk | JDK only |
| axiom-runtime-netty | io.netty:netty-all |
| axiom-json-jackson | com.fasterxml.jackson.core |
| axiom-json-gson | com.google.code.gson |

### 11.2 JPMS Modules

```java
// module-info.java for axiom-core
module io.axiom.core {
    exports io.axiom.core.handler;
    exports io.axiom.core.context;
    exports io.axiom.core.routing;
    exports io.axiom.core.middleware;
    exports io.axiom.core.lifecycle;
    exports io.axiom.core.error;
    exports io.axiom.core.app;
}
```

---

## 12. API Stability

### 12.1 Stability Tiers

| Package | Stability |
|---------|-----------|
| `io.axiom.core.*` | Stable after 1.0 |
| `io.axiom.http.*` | Stable after 1.0 |
| `io.axiom.*.internal` | Internal, may change |
| `io.axiom.runtime.*` | Runtime contract stable |

### 12.2 Deprecation Policy

1. Deprecated in minor version
2. Removed in next major version
3. Migration guide provided
4. Compiler warnings enabled

---

## 13. References

- [RFC-0001: Core Framework Design](../../draft/RFC_0001.md)
- [RFC-0002: Routing & Composition](../../draft/RFC_0002.md)
- [RFC-0003: Routing Matcher Algorithm](../../draft/RFC_0003.md)
- [RFC-0004: Middleware Pipeline](../../draft/RFC_0004.md)
- [RFC-0005: DX Philosophy](../../draft/RFC_0005.md)
- [RFC-0006: Build Tool Strategy](../../draft/RFC_0006.md)

---

*This document is the architectural source of truth for Axiom.*
