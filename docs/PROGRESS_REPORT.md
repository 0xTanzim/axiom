# Axiom Framework — Progress Report

**Date:** January 18, 2026
**Version:** 0.1.0-SNAPSHOT
**Tests:** 305 passing ✅
**Modules:** 6 (core, server, persistence, persistence-processor, config, validation)
**Java Target:** 25 (LTS)

---

## Executive Summary

Axiom is **~95% complete** against its RFC specification. All core features are production-ready:

- ✅ Core engine (routing, middleware, context, app composition)
- ✅ Lifecycle management with hooks
- ✅ JDK runtime adapter with virtual threads
- ✅ **Persistence** with transactions, JDBC, JPA, jOOQ
- ✅ **Configuration** with .env, profiles, type-safe bindings
- ✅ **Validation** with Jakarta Validation / Hibernate Validator
- ✅ **Logging** with SLF4J 2.x + MDC request correlation

The only gap is **RFC-0010: Testing Utilities**.

---

## Module Status

| Module | Tests | Status | Description |
|--------|-------|--------|-------------|
| `axiom-core` | 135 | ✅ Complete | Handler, Context, Router, Middleware, App, Lifecycle, Error |
| `axiom-server` | 9 | ✅ Complete | JDK HttpServer adapter, virtual threads, MDC |
| `axiom-persistence` | 54 | ✅ Complete | Transaction, JDBC, JPA, jOOQ, TransactionalValidator |
| `axiom-persistence-processor` | 16 | ✅ Complete | @Transactional compile-time code generation |
| `axiom-config` | 43 | ✅ Complete | SmallRye Config, .env, profiles, type-safe |
| `axiom-validation` | 48 | ✅ Complete | Hibernate Validator, ValidationResult |
| **Total** | **305** | ✅ | All tests passing |

---

## RFC Implementation Status

| RFC | Title | Status | Module(s) |
|-----|-------|--------|-----------|
| RFC-0001 | Core Primitives | ✅ Complete | axiom-core |
| RFC-0002 | Router & App | ✅ Complete | axiom-core |
| RFC-0003 | Trie-based Routing | ✅ Complete | axiom-core |
| RFC-0004 | Middleware Pipeline | ✅ Complete | axiom-core |
| RFC-0005 | DX Philosophy | ✅ Applied | All |
| RFC-0006 | Build Tool Agnostic | ✅ Complete | All |
| RFC-0007 | Lifecycle Management | ✅ Complete | axiom-core |
| RFC-0008 | Error Handling | ✅ Complete | axiom-core |
| RFC-0009 | Server SPI | ✅ Complete | axiom-core, axiom-server |
| RFC-0010 | Testing Utilities | ❌ Not Started | - |
| RFC-0011 | Persistence | ✅ Complete | axiom-persistence, axiom-persistence-processor |
| RFC-0012 | Logging (SLF4J) | ✅ **NEW** Complete | axiom-core, axiom-server |
| RFC-0013 | Configuration | ✅ Complete | axiom-config |
| RFC-0014 | Validation | ✅ Complete | axiom-validation |

---

## What Works Today

### Complete HTTP Framework

```java
App app = Axiom.create();

// Routing with path params
Router router = new Router();
router.get("/health", ctx -> ctx.text("OK"));
router.get("/users/:id", ctx -> ctx.json(userService.find(ctx.param("id"))));
router.post("/users", ctx -> {
    User user = ctx.body(User.class);
    ctx.status(201);
    ctx.json(userService.create(user));
});

// Middleware (pick your style)
app.use((ctx, next) -> {
    LOG.info("{} {}", ctx.method(), ctx.path());
    next.run();
});

app.route(router);
app.listen(8080);
```

### Configuration System

```java
// application.properties
// server.port=8080
// server.host=0.0.0.0

@ConfigMapping(prefix = "server")
interface ServerConfig {
    String host();
    int port();
    @WithDefault("30s")
    Duration timeout();
}

AxiomConfig config = AxiomConfig.builder()
    .withMapping(ServerConfig.class)
    .build();

ServerConfig server = config.bind(ServerConfig.class);
```

### Validation System

```java
record CreateUserRequest(
    @NotBlank String name,
    @Email @NotBlank String email,
    @Min(18) int age
) {}

// Validate and handle
ValidationResult<CreateUserRequest> result = AxiomValidator.validate(request);
if (!result.isValid()) {
    ctx.status(400);
    ctx.json(result.errors());
    return;
}

// Or throw on invalid
CreateUserRequest valid = AxiomValidator.validateOrThrow(request);
```

### Persistence with Transactions

```java
// Define transactional service
public class UserRepository {
    @Transactional
    public void save(User user) {
        Jdbc.execute(ds, "INSERT INTO users ...", user.name(), user.email());
    }
}

// Use generated wrapper (compile-time, zero reflection)
UserRepository$Tx repo = new UserRepository$Tx(dataSource);
repo.save(user);  // Auto-wrapped in transaction
```

### SLF4J Logging with MDC

```java
// Framework automatically logs:
// INFO  io.axiom.core.app.DefaultApp - Starting Axiom application...
// INFO  io.axiom.server.JdkServer - JdkServer listening on 0.0.0.0:8080 (virtual threads: true)
// DEBUG io.axiom.server.JdkServer - [req-a1b2c3d4] GET /users/123
// DEBUG io.axiom.core.routing.Router - Route matched: GET /users/123 -> /users/:id

// Users configure via logback.xml:
// <pattern>%d{HH:mm:ss.SSS} %-5level [%X{requestId}] %logger{36} - %msg%n</pattern>
```

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      User Application                        │
├─────────────────────────────────────────────────────────────┤
│ axiom-validation │ axiom-config │ axiom-persistence          │
│   (optional)     │  (optional)  │    (optional)              │
├──────────────────┴──────────────┴────────────────────────────┤
│                      axiom-server                            │
│              (JDK HttpServer + Virtual Threads)              │
├─────────────────────────────────────────────────────────────┤
│                       axiom-core                             │
│  Handler │ Context │ Router │ Middleware │ App │ Lifecycle   │
└─────────────────────────────────────────────────────────────┘
```

### Module Dependencies

```
axiom-core (foundation, depends only on JDK + Jackson + SLF4J)
    ↑
axiom-server (depends on axiom-core)
    ↑
axiom-persistence (depends on axiom-core)
axiom-config (standalone, depends on SmallRye Config)
axiom-validation (standalone, depends on Hibernate Validator)

axiom-persistence-processor (compile-time only, generates code)
```

---

## Quality Metrics

| Metric | Value | Target | Status |
|--------|-------|--------|--------|
| Tests | 305 | >250 | ✅ Exceeded |
| Test Coverage | ~85% | >80% | ✅ Met |
| Build Time | ~8s | <30s | ✅ Fast |
| Java Version | 25 | 25 LTS | ✅ Current |
| System.out/err in code | 0 | 0 | ✅ Clean |
| SLF4J logging | Yes | Yes | ✅ Complete |

---

## Recent Completions (This Session)

1. ✅ **DX Issue #2 Fixed** — Added `TransactionalValidator` with 10 new tests
2. ✅ **Example App** — Complete auth example (`examples/axiom-auth-example/`)
3. ✅ **Hot Reload** — Documented JBang, DevTools, JRebel, DCEVM options
4. ✅ **Maven/Lombok** — Full compatibility documented in ROADMAP

---

## Next Steps

See [ROADMAP.md](ROADMAP.md) for detailed next phase planning.

**Immediate Priority:** RFC-0010 Testing Utilities
