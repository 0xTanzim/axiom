# Axiom Framework — Progress Report

**Date:** January 17, 2026
**Current Version:** 0.1.0-SNAPSHOT
**Tests:** 144 passing ✅
**Build:** Maven + JPMS enabled

---

## Executive Summary

Axiom is **~85% complete** against its RFC specification. The core engine (routing, middleware, context, app composition), lifecycle management, and JDK runtime adapter are production-ready. The main gaps are **testing utilities** and **persistence layer** (RFC-0011).

| Area | Progress | Status |
|------|----------|--------|
| Core Engine | 100% | ✅ Complete |
| Routing System | 100% | ✅ Complete |
| Middleware Pipeline | 100% | ✅ Complete |
| Server SPI | 100% | ✅ Complete |
| JDK Runtime | 100% | ✅ Complete |
| Lifecycle Hooks | 100% | ✅ **NEW** Complete |
| ServerConfig | 100% | ✅ **NEW** Complete |
| Error Handling | 100% | ✅ Complete |
| Testing Utilities | 0% | ❌ NOT DONE |
| Persistence Layer | 0% | ❌ RFC-0011 Draft |

---

## What Works Today

```java
App app = Axiom.create();

Router router = new Router();
router.get("/health", ctx -> ctx.text("OK"));
router.get("/users/:id", ctx -> {
    String id = ctx.param("id");
    ctx.json(Map.of("id", id, "name", "User " + id));
});

router.post("/users", ctx -> {
    var body = ctx.body(UserRequest.class);
    ctx.status(201);
    ctx.json(userService.create(body));
});

// Middleware (pick your style)
app.use((ctx, next) -> {
    log.info("{} {}", ctx.method(), ctx.path());
    next.run();
});

app.use(ctx -> {
    ctx.header("X-Framework", "Axiom");
    ctx.next();
});

// Lifecycle hooks (NEW!)
app.onStart(() -> {
    database.connect();
    cache.warm();
});

app.onReady(() -> {
    log.info("Server ready at http://localhost:{}", app.port());
});

app.onShutdown(() -> {
    database.close();
    cache.flush();
});

app.onLifecycleError(e -> {
    alerting.send("Server failed: " + e.getMessage());
});

app.route(router);
app.onError((ctx, e) -> {
    ctx.status(500);
    ctx.json(Map.of("error", e.getMessage()));
});

// Custom server config (NEW!)
ServerConfig config = ServerConfig.builder()
    .host("0.0.0.0")
    .port(8080)
    .maxRequestSize(10 * 1024 * 1024)  // 10MB
    .shutdownTimeout(Duration.ofSeconds(30))
    .virtualThreads(true)
    .build();

app.listen(config);
```

---

## RFC Implementation Status

| RFC | Title | Status | Details |
|-----|-------|--------|---------|
| RFC-0001 | Core Design & Handler API | ✅ **DONE** | Handler, Context, DefaultContext |
| RFC-0002 | Routing & App Composition | ✅ **DONE** | Router, App, DefaultApp, Axiom factory |
| RFC-0003 | Routing Matcher Algorithm | ✅ **DONE** | Trie-based O(depth) matching |
| RFC-0004 | Middleware Pipeline | ✅ **DONE** | Dual style: `(ctx, next)` + `ctx.next()` |
| RFC-0005 | DX Philosophy | ✅ **Applied** | Style freedom, minimal API |
| RFC-0006 | Build Tool Strategy | ✅ **DONE** | Plain JAR, no plugin required |
| RFC-0007 | Lifecycle Management | ✅ **DONE** | onStart/onReady/onShutdown/onError + ServerConfig |
| RFC-0008 | Error Handling Architecture | ✅ **DONE** | Structured error flow |
| RFC-0009 | Runtime Adapter Contract | ✅ **DONE** | Server SPI + JDK HttpServer |
| RFC-0010 | Testing Utilities | ❌ **NOT DONE** | MockContext, AppTester |
| RFC-0011 | Persistence & Transaction | 📝 **Draft** | JPA + jOOQ + JDBC (future) |

---

## Module Structure

```
axiom/
├── axiom-core/              ← Core abstractions
│   └── io.axiom.core/
│       ├── app/             App, DefaultApp, Axiom factory
│       ├── context/         Context, DefaultContext
│       ├── error/           AxiomException hierarchy
│       ├── handler/         Handler interface
│       ├── json/            JsonCodec, JacksonCodec
│       ├── lifecycle/       LifecyclePhase, ThrowingRunnable, Exceptions (NEW)
│       ├── middleware/      Middleware, MiddlewareHandler, SimpleMiddleware
│       ├── routing/         Router, RouteTrie, Segment types
│       └── server/          Server, ServerFactory, ServerConfig (NEW)
│
└── axiom-server/            ← JDK HttpServer runtime
    └── io.axiom.server/
        └── JdkServer         Full lifecycle + ServerConfig support
```

---

## Recent Changes (January 17, 2026)

### Lifecycle Management (RFC-0007)

**Added lifecycle package with:**
- `LifecyclePhase` enum: INIT → STARTING → STARTED → STOPPING → STOPPED → ERROR
- `ThrowingRunnable` functional interface for hooks that may throw
- `LifecycleException`, `StartupException`, `ShutdownException`

**Updated App interface:**
- `onStart(ThrowingRunnable)` - runs during STARTING phase
- `onReady(Runnable)` - runs after STARTED, non-blocking
- `onShutdown(ThrowingRunnable)` - runs during STOPPING (LIFO order)
- `onLifecycleError(Consumer<Throwable>)` - runs on ERROR state
- `listen(ServerConfig)` - custom server configuration
- `phase()` - returns current lifecycle phase

**Updated DefaultApp:**
- Complete lifecycle state machine with AtomicReference
- Thread-safe state transitions
- Shutdown hooks execute in reverse registration order
- Ready hook failures logged but don't affect state
- Error hooks run when ERROR state is entered

### ServerConfig

**New immutable configuration record:**
- `host` - bind address (default: 0.0.0.0)
- `port` - listen port (default: 8080)
- `maxRequestSize` - max body size (default: 10MB)
- `readTimeout` - read timeout (default: 30s)
- `writeTimeout` - write timeout (default: 30s)
- `shutdownTimeout` - graceful shutdown timeout (default: 30s)
- `drainTimeout` - drain in-flight requests (default: 10s)
- `virtualThreads` - use virtual threads (default: true)

### JdkServer Updates

- Implements `start(ServerConfig)` as primary method
- Uses config values for all tuning parameters
- Graceful shutdown with configurable drain timeout
- Virtual thread executor when enabled

---

## Test Coverage

**Total: 144 tests passing**

| Module | Tests | Status |
|--------|-------|--------|
| axiom-core | 135 | ✅ Pass |
| axiom-server | 9 | ✅ Pass |

### Test Categories
- DefaultApp (19 tests): middleware, routing, error handling, lifecycle
- DefaultContext (20 tests): request/response methods, state management
- Router (19 tests): matching, grouping, merging, validation
- RouteTrie (17 tests): insert, match, allowed methods
- Middleware (7 tests): compose, apply, identity
- SimpleMiddleware (9 tests): adapter, context next, error handling
- JacksonCodec (8 tests): serialize, deserialize
- ErrorTypes (10 tests): exception hierarchy
- JdkServer (9 tests): integration tests

---

## What's Next

### Phase 1: Testing Utilities (RFC-0010)
- MockContext for unit testing handlers
- AppTester for integration testing
- TestClient for HTTP-level testing

### Phase 2: Persistence Layer (RFC-0011)
- JPA/Hibernate integration
- jOOQ integration
- Plain JDBC support
- `@Transactional` with compile-time AOP
- Mix ORM + jOOQ + JDBC in same transaction

---

## Architecture Highlights

### Virtual Threads
Java 25 LTS with virtual threads enables millions of concurrent connections without thread pool tuning. JEP 491 removes synchronized-block pinning.

### Zero-Reflection Core
Core framework uses no reflection. All composition is explicit through interfaces and functional types.

### SPI-Based Runtime Discovery
Server implementations are discovered via ServiceLoader. Users add axiom-server dependency and runtime is auto-discovered.

### Explicit Lifecycle
6-phase lifecycle with hooks at each transition:
- INIT: Configuration phase
- STARTING: Startup hooks run
- STARTED: Accepting requests
- STOPPING: Shutdown hooks run (LIFO)
- STOPPED: Terminal state
- ERROR: Failure state with error hooks
