# Axiom Framework — Roadmap

**Date:** January 17, 2026
**Current Progress:** ~85% of core RFC scope
**Tests:** 144 passing

---

## Current State Summary

### ✅ Completed (Phase 1-3)

| RFC | Feature | Status |
|-----|---------|--------|
| RFC-0001 | Handler, Context, DefaultContext | ✅ Complete |
| RFC-0002 | Router, App, Axiom factory | ✅ Complete |
| RFC-0003 | Trie-based routing | ✅ Complete |
| RFC-0004 | Middleware pipeline (dual style) | ✅ Complete |
| RFC-0005 | DX philosophy applied | ✅ Complete |
| RFC-0006 | Build tool agnostic | ✅ Complete |
| RFC-0007 | Lifecycle management | ✅ Complete |
| RFC-0008 | Structured error flow | ✅ Complete |
| RFC-0009 | Server SPI + JDK adapter | ✅ Complete |

### ❌ Not Implemented

| RFC | Feature | Priority |
|-----|---------|----------|
| RFC-0012 | Logging (SLF4J) | **P0** |
| RFC-0010 | Testing utilities | **P0** |
| RFC-0011 | Persistence & transactions | P1 |
| RFC-0013 | Configuration system | P1 |
| RFC-0014 | Validation (JSR-380) | P2 |

---

## Phase 4: Logging Integration (RFC-0012) — NEW

**Priority:** P0 (Production Requirement)
**Estimated effort:** 2-3 days

### Why This First?

Current state: `System.err.println()` — NOT production-ready.

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

## Phase 5: Testing Utilities (RFC-0010)

**Priority:** P1
**Estimated effort:** 1 week

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

## Phase 6: Persistence Layer (RFC-0011)

**Priority:** P1
**Estimated effort:** 3-4 weeks

RFC-0011 defines a comprehensive persistence and transaction system.

### Core Principles

1. **Single module for users:** `axiom-persistence`
2. **JPA/Hibernate as primary** — most developers expect ORM
3. **jOOQ and JDBC as first-class** — not second-class citizens
4. **Mix freely** — ORM + jOOQ + JDBC in same transaction
5. **Framework magic for infrastructure only** — not application logic
6. **Use existing libraries** — HikariCP, Hibernate, Flyway

### Architecture

```
axiom-persistence/
├── core/          # Transaction abstraction, DataSource
├── jpa/           # JPA/Hibernate integration
├── jooq/          # jOOQ integration
├── jdbc/          # Plain JDBC support
└── flyway/        # Migration support
```

### Key Features

#### Zero-Config Startup
```java
AxiomPersistence.start();  // That's it
```

#### Transaction Management
```java
@Transactional
public void createOrder(Order order) {
    // JPA for entity persistence
    entityManager.persist(order);

    // jOOQ for complex query
    dsl.update(INVENTORY)
       .set(INVENTORY.QUANTITY, INVENTORY.QUANTITY.minus(order.quantity()))
       .where(INVENTORY.PRODUCT_ID.eq(order.productId()))
       .execute();

    // Plain JDBC for legacy integration
    jdbc.execute("INSERT INTO audit_log VALUES (?, ?, ?)", ...);
}
```

### Implementation Strategy

1. **HikariCP as default pool** — industry standard
2. **Scoped Values for transaction binding** — Java 25 feature, better than ThreadLocal
3. **Compile-time AOP** — @Transactional without runtime proxies
4. **Flyway integration** — automatic migrations

---

## Phase 7: Configuration System (RFC-0013) — PLANNED

**Priority:** P1
**Estimated effort:** 1 week

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

## Future Considerations (Beyond MVP)

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
| Phase 4: Logging (SLF4J) | January 2026 | 📋 Next |
| Phase 5: Testing Utilities | February 2026 | 📋 Planned |
| Phase 6: Persistence Layer | Q1 2026 | 📝 RFC Draft |
| Phase 7: Configuration System | Q1 2026 | 📋 Planned |
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
| RFC-0011 | Persistence & Transaction | 📝 Draft |
| RFC-0012 | Logging (SLF4J) | 📝 **NEW** Draft |
| RFC-0013 | Configuration System | 📋 Planned |
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
| RFC-0010 | Testing Utilities | 📋 Next |
| RFC-0011 | Persistence & Transaction | 📝 Draft |
