# Axiom Framework — Roadmap

**Date:** January 2026
**Current Progress:** ~90% of core RFC scope
**Tests:** 204 passing (135 core + 9 server + 44 persistence + 16 processor)

---

## Current State Summary

### ✅ Completed (Phase 1-3 + Persistence)

| RFC | Feature | Status | Tests |
|-----|---------|--------|-------|
| RFC-0001 | Handler, Context, DefaultContext | ✅ Complete | 135 |
| RFC-0002 | Router, App, Axiom factory | ✅ Complete | ↑ |
| RFC-0003 | Trie-based routing | ✅ Complete | ↑ |
| RFC-0004 | Middleware pipeline (dual style) | ✅ Complete | ↑ |
| RFC-0005 | DX philosophy applied | ✅ Complete | - |
| RFC-0006 | Build tool agnostic | ✅ Complete | - |
| RFC-0007 | Lifecycle management | ✅ Complete | ↑ |
| RFC-0008 | Structured error flow | ✅ Complete | ↑ |
| RFC-0009 | Server SPI + JDK adapter | ✅ Complete | 9 |
| RFC-0011 | Persistence & transactions | ✅ **COMPLETE** | 60 |

### ❌ Not Yet Implemented

| RFC | Feature | Priority | Status |
|-----|---------|----------|--------|
| RFC-0012 | Logging (SLF4J) | **P0 CRITICAL** | RFC exists, needs implementation |
| RFC-0013 | Configuration system | P1 | **RFC NEEDED** |
| RFC-0010 | Testing utilities | P1 | RFC exists, needs implementation |
| RFC-0014 | Validation (JSR-380) | P2 | Future |

---

## Phase 4: Logging Integration (RFC-0012) — NEXT

**Priority:** P0 CRITICAL (Production Requirement)
**Estimated effort:** 2-3 days
**RFC Status:** ✅ RFC exists at `draft/RFC_0012.md`

### Why This First?

Current state: `System.err.println()` in `DefaultApp.java` — **NOT production-ready**.

Found 5 occurrences:
- Line 286: Exception handling
- Line 364: Ready hook failure
- Line 384: Stop hook failure
- Line 455: Shutdown handler failure
- Line 496: Request handler error

### What to Add

```java
// Replace this (current)
System.err.println("Ready hook failed: " + e.getMessage());

// With this (SLF4J)
private static final Logger LOG = LoggerFactory.getLogger(DefaultApp.class);
LOG.warn("Ready hook failed: {}", e.getMessage(), e);
```

### Features

1. SLF4J 2.x as logging facade
2. User provides implementation (Logback recommended)
3. MDC for request correlation
4. Appropriate log levels throughout framework

---

## Phase 5: Configuration System (RFC-0013) — RFC NEEDED

**Priority:** P1
**Estimated effort:** 1 week
**RFC Status:** ❌ RFC document doesn't exist yet

### What to Add

```properties
# application.properties
server.port=8080
server.host=0.0.0.0

axiom.datasource.url=jdbc:postgresql://localhost/mydb
axiom.datasource.username=user
axiom.datasource.password=pass
axiom.datasource.pool.size=10
```

- Properties file support
- Environment variable override
- Type-safe config objects
- Programmatic override still available

---

## Phase 6: Testing Utilities (RFC-0010)

**Priority:** P1
**Estimated effort:** 1 week
**RFC Status:** ✅ RFC exists at `draft/RFC_0010.md`

### What to Add

#### 1. MockContext

```java
// For unit testing handlers in isolation
MockContext ctx = MockContext.builder()
    .method("GET")
    .path("/users/123")
    .param("id", "123")
    .query("page", "1")
    .header("Authorization", "Bearer token")
    .body("{\"name\":\"John\"}")
    .build();

handler.handle(ctx);

assertThat(ctx.responseStatus()).isEqualTo(200);
assertThat(ctx.responseBody()).contains("John");
```

#### 2. AppTester

```java
// For integration testing through the full stack
AppTester tester = AppTester.create(app);

tester.get("/users/123")
    .assertStatus(200)
    .assertJson(json -> json.path("id").isEqualTo("123"));

tester.post("/users")
    .body(new User("John"))
    .assertStatus(201)
    .assertHeader("Location", "/users/1");
```

#### 3. TestClient

```java
// For HTTP-level testing with real server
TestClient client = TestClient.create(app);
client.start();  // Starts on random port

Response response = client.get("/health");
assertThat(response.status()).isEqualTo(200);

client.stop();
```

### Files to Create

| File | Location |
|------|----------|
| `MockContext.java` | `io.axiom.core.test` |
| `AppTester.java` | `io.axiom.core.test` |
| `TestClient.java` | `io.axiom.core.test` |
| `Assertions.java` | `io.axiom.core.test` |

---

## Phase 7: Persistence Layer (RFC-0011) — ✅ COMPLETE

**Priority:** P1
**Status:** ✅ **COMPLETE** (60 tests passing)
**RFC:** `draft/RFC_0011.md`

RFC-0011 defines a comprehensive persistence and transaction system.

### ✅ Implemented Features

1. **Zero-Config Startup**
```java
AxiomPersistence.start();  // That's it - auto-discovers config
```

2. **Transaction Management**
```java
Transaction.execute(() -> {
    entityManager.persist(order);
    dsl.update(INVENTORY)...
    jdbc.execute("INSERT INTO audit_log...");
});
```

3. **Compile-time AOP** - @Transactional without runtime proxies
4. **Scoped Values** - Java 25 feature for transaction binding
5. **HikariCP** - Industry standard connection pooling
6. **Flyway** - Automatic migrations
7. **JPA + jOOQ + JDBC** - Mix freely in same transaction

### Architecture

```
axiom-persistence/           (44 tests)
├── core/          # Transaction abstraction, DataSource
├── jpa/           # JPA/Hibernate integration
├── jooq/          # jOOQ integration
├── jdbc/          # Plain JDBC support
└── flyway/        # Migration support

axiom-persistence-processor/ (16 tests)
└── @Transactional annotation processor
```

---

## Phase 8: Future RFCs

### Validation (RFC-0014)
- Bean Validation (JSR-380) integration
- Hibernate Validator
- `ctx.validBody(CreateUserRequest.class)`

### Observability (RFC-0015)
- Micrometer metrics integration
- OpenTelemetry tracing
- Health check endpoints

### Additional Runtime Adapters
- Netty-based runtime for high throughput
- Undertow adapter

### Security
- Authentication middleware
- Authorization framework
- CORS configuration
- Rate limiting

---

## Milestone Timeline

| Phase | Target | Status |
|-------|--------|--------|
| Phase 1: Core Engine | January 2026 | ✅ Complete |
| Phase 2: Routing + Middleware | January 2026 | ✅ Complete |
| Phase 3: Lifecycle + Config | January 2026 | ✅ Complete |
| Phase 4: Logging (SLF4J) | January 2026 | 📋 **NEXT** |
| Phase 5: Configuration System | January 2026 | 📝 RFC Needed |
| Phase 6: Testing Utilities | February 2026 | 📋 Planned |
| Phase 7: Persistence Layer | January 2026 | ✅ **COMPLETE** |
| MVP Release (0.1.0) | Q2 2026 | 🎯 Target |

---

## RFC Index

| RFC | Title | Status |
|-----|-------|--------|
| RFC-0001 | Core Design & Handler API | ✅ Implemented |
| RFC-0002 | Routing & App Composition | ✅ Implemented |
| RFC-0003 | Routing Matcher Algorithm | ✅ Implemented |
| RFC-0004 | Middleware Pipeline | ✅ Implemented |
| RFC-0005 | DX Philosophy | ✅ Applied |
| RFC-0006 | Build Tool Strategy | ✅ Implemented |
| RFC-0007 | Lifecycle Management | ✅ Implemented |
| RFC-0008 | Error Handling Architecture | ✅ Implemented |
| RFC-0009 | Runtime Adapter Contract | ✅ Implemented |
| RFC-0010 | Testing Utilities | 📋 Planned |
| RFC-0011 | Persistence & Transaction | ✅ **COMPLETE** |
| RFC-0012 | Logging (SLF4J) | 📋 **NEXT** |
| RFC-0013 | Configuration System | 📝 **RFC NEEDED** |
| RFC-0014 | Validation (JSR-380) | 📋 Future |
| RFC-0015 | Observability | 📋 Future |

---

## Key Design Principles

### Don't Reinvent — Integrate

| Need | Use |
|------|-----|
| Logging | SLF4J + Logback |
| Connection Pool | HikariCP |
| ORM | Hibernate/JPA |
| JSON | Jackson ✅ |
| Validation | Hibernate Validator |
| Migrations | Flyway |
| HTTP Client | Java HttpClient |

### Infrastructure Magic, Not Application Magic

- **Automatic (framework handles):** Logging, transactions, lifecycle, connections
- **Explicit (developer controls):** HTTP routing, handlers, business logic

### Java 25 Features

- Virtual Threads ✅ (JdkServer)
- Records ✅ (ServerConfig, DTOs)
- Scoped Values (for transactions — planned)
- Pattern Matching (where applicable)

---

## Contributing

1. Check RFC in `/draft` before implementing
2. Follow code style in `.github/copilot-instructions.md`
3. Add tests for new functionality
4. Update documentation

---

## Lombok Compatibility

Axiom is **naturally compatible** with Lombok. No framework changes needed.

### Why It Works

- Lombok is a compile-time annotation processor
- It generates bytecode at compile time (getters, setters, builders)
- Axiom sees regular Java classes at runtime
- No runtime reflection conflicts

### Recommendation

| Use Case | Recommendation |
|----------|----------------|
| Simple DTOs | Java Records (Java 25 native) |
| Complex entities | Lombok `@Data`, `@Builder` |
| JPA entities | Lombok `@Getter`, `@Setter` (avoid `@Data` with JPA) |

### Example

```java
// Works perfectly with Axiom
@Data
@Builder
public class User {
    private Long id;
    private String name;
    private String email;
}

// Handler using Lombok-generated class
ctx.json(User.builder().id(1L).name("John").build());
```
