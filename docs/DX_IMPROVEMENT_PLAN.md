# Axiom Framework — DX Improvement Plan

**Date:** January 17, 2026
**Scope:** Developer Experience Analysis & Recommendations
**Version:** 0.1.0-SNAPSHOT

---

## Executive Summary

This document analyzes Axiom's current developer experience (DX) and identifies improvements needed to make the framework production-ready with excellent DX. The core principle remains:

> **"Axiom provides infrastructure magic, not application magic."**

HTTP routing and handlers = explicit (developer controls)
Persistence, transactions, logging, lifecycle = automatic (framework handles)

---

## 1. Current DX State

### ✅ What Works Well

| Area | DX Quality | Notes |
|------|------------|-------|
| Handler API | ⭐⭐⭐⭐⭐ | Clean `ctx -> ctx.json()` pattern |
| Middleware | ⭐⭐⭐⭐⭐ | Dual style: `(ctx, next)` and `ctx.next()` |
| Routing | ⭐⭐⭐⭐⭐ | Explicit, type-safe, no magic |
| Lifecycle | ⭐⭐⭐⭐ | New hooks: onStart/onReady/onShutdown |
| ServerConfig | ⭐⭐⭐⭐ | Builder pattern, sensible defaults |
| Error Handling | ⭐⭐⭐⭐ | Centralized, customizable |

### ❌ What's Missing or Weak

| Area | Current State | Impact |
|------|---------------|--------|
| Logging | `System.err.println()` | ❌ Not production-ready |
| Configuration | Code-only | ❌ No external config |
| Testing | Nothing | ❌ Hard to test handlers |
| Validation | Manual | ⚠️ Boilerplate |
| Persistence | Nothing | ❌ Users must DIY |
| Observability | Nothing | ⚠️ No metrics/tracing |

---

## 2. Critical DX Gaps

### 2.1 Logging — CRITICAL

**Current State:**
```java
// From DefaultApp.java - NOT production-ready
System.err.println("Ready hook failed: " + e.getMessage());
System.err.println("Shutdown completed with " + failures.size() + " error(s)");
```

**What Users Expect:**
```java
private static final Logger log = LoggerFactory.getLogger(MyService.class);

log.info("Server starting on port {}", port);
log.debug("Processing request: {} {}", method, path);
log.error("Failed to handle request", exception);
```

**Solution:** SLF4J integration (RFC-0012)

**Why SLF4J?**
- Industry standard facade
- Users choose implementation (Logback, Log4j2)
- Zero config to start
- Full control when needed
- Every Java developer knows it

**DO NOT:** Create custom logging API. Use existing standard.

---

### 2.2 Configuration — IMPORTANT

**Current State:**
```java
// Only code-based configuration
ServerConfig config = ServerConfig.builder()
    .port(8080)
    .build();
```

**What Users Expect:**
```properties
# application.properties
server.port=8080
server.host=0.0.0.0
database.url=jdbc:postgresql://localhost:5432/mydb
database.pool.size=10
```

**Solution:** External configuration support (RFC-0013)

**Approach:**
1. Support `application.properties` by default
2. Allow YAML as alternative
3. Environment variable override: `SERVER_PORT=8080`
4. Programmatic override still possible
5. Type-safe config objects

---

### 2.3 Testing Utilities — CRITICAL

**Current State:** Nothing. Users cannot easily test handlers.

**What Users Expect:**
```java
// Unit test a handler
@Test
void testGetUser() {
    MockContext ctx = MockContext.get("/users/123")
        .param("id", "123")
        .build();

    handler.handle(ctx);

    assertThat(ctx.status()).isEqualTo(200);
    assertThat(ctx.bodyAs(User.class).id()).isEqualTo("123");
}

// Integration test
@Test
void testFullStack() {
    AppTester tester = AppTester.of(app);

    tester.get("/users/123")
        .assertStatus(200)
        .assertJson(json -> json.path("id").isEqualTo("123"));
}
```

**Solution:** Complete RFC-0010 Testing Utilities

---

### 2.4 Validation — NICE TO HAVE

**Current State:** Manual validation in every handler.

**What Users Expect:**
```java
public record CreateUserRequest(
    @NotBlank String email,
    @Size(min = 8) String password,
    @NotNull String name
) {}

// In handler - automatic validation
CreateUserRequest req = ctx.validBody(CreateUserRequest.class);
// Throws ValidationException if invalid
```

**Solution:** Bean Validation (JSR-380) integration (RFC-0014)

**DO NOT:** Create custom validation framework. Use Hibernate Validator.

---

## 3. RFC-0011 Evaluation

### ✅ Good Design Decisions

1. **Single user-facing module:** `axiom-persistence`
   - Users don't think about internal structure
   - Clean dependency

2. **"Zero noise" startup:** `AxiomPersistence.start()`
   - Infrastructure is boring and automatic
   - No boilerplate

3. **Both annotation and explicit styles:**
   ```java
   // Style A: Standard JPA
   @PersistenceContext
   private EntityManager em;

   // Style B: Explicit Axiom
   private final EntityManager em = Jpa.em();
   ```
   - Flexibility for different preferences

4. **Mix ORM + jOOQ + JDBC in same transaction:**
   - Powerful feature
   - Real-world need

5. **Compile-time AOP only:**
   - No runtime proxy overhead
   - Predictable performance

### ⚠️ Gaps in RFC-0011

| Gap | Recommendation |
|-----|----------------|
| Configuration mechanism unclear | Define properties-based config |
| Connection pool not specified | Default to HikariCP |
| AOP tooling not specified | Define annotation processor approach |
| Migration support missing | Integrate Flyway |
| Multi-datasource DX unclear | Add detailed examples |
| Test support missing | Add @Rollback for tests |

### Recommended RFC-0011 Additions

```java
// 1. Configuration (add to RFC)
# application.properties
axiom.datasource.url=jdbc:postgresql://localhost/mydb
axiom.datasource.username=user
axiom.datasource.password=pass
axiom.datasource.pool.size=10

axiom.jpa.hibernate.ddl-auto=validate
axiom.jpa.show-sql=false

// 2. Migration support (add to RFC)
# Flyway runs automatically on startup
axiom.flyway.enabled=true
axiom.flyway.locations=classpath:db/migration

// 3. Test support (add to RFC)
@Test
@Transactional  // Auto-rollback after test
void testCreateUser() {
    userService.create(new User("test@example.com"));
    // Changes rolled back automatically
}
```

---

## 4. Java 25 Features to Leverage

### Currently Using
- ✅ Virtual Threads (JdkServer)
- ✅ Records (ServerConfig, DTOs)
- ✅ Sealed interfaces (where applicable)

### Should Add

| Feature | Use Case | Priority |
|---------|----------|----------|
| **Scoped Values** | Transaction binding (replace ThreadLocal) | P0 |
| **Pattern Matching** | Error handling, type dispatch | P1 |
| **Sequenced Collections** | Middleware order, hook order | P2 |
| **String Templates** | SQL building, error messages | P2 |

### Scoped Values for Transactions

```java
// Current approach (ThreadLocal - problematic with virtual threads)
private static final ThreadLocal<Transaction> TX = new ThreadLocal<>();

// Java 25 approach (Scoped Values - designed for virtual threads)
private static final ScopedValue<Transaction> TX = ScopedValue.newInstance();

@Transactional
public void doWork() {
    ScopedValue.runWhere(TX, new Transaction(), () -> {
        // Transaction available here
        userRepo.save(user);
    });
}
```

---

## 5. What NOT to Build (Use Existing)

| Need | DON'T Build | USE Instead |
|------|-------------|-------------|
| Logging | Custom logger | SLF4J + Logback |
| Connection Pool | Custom pool | HikariCP |
| ORM | Custom ORM | Hibernate/JPA |
| JSON | Custom serializer | Jackson ✅ (already using) |
| Validation | Custom validation | Hibernate Validator |
| Migrations | Custom migrations | Flyway |
| HTTP Client | Custom client | Java HttpClient |
| Testing | Custom mocks | JUnit 5 + AssertJ |

**Axiom's job:** Integrate these well, not replace them.

---

## 6. Recommended RFC Roadmap

### Immediate (Before Persistence)

| RFC | Title | Priority | Rationale |
|-----|-------|----------|-----------|
| RFC-0012 | Logging (SLF4J) | P0 | Production requirement |
| RFC-0010 | Testing Utilities | P0 | Quality requirement |

### Short-term (With Persistence)

| RFC | Title | Priority | Rationale |
|-----|-------|----------|-----------|
| RFC-0011 | Persistence & Transactions | P0 | User expectation |
| RFC-0013 | Configuration System | P1 | Usability |

### Medium-term

| RFC | Title | Priority | Rationale |
|-----|-------|----------|-----------|
| RFC-0014 | Validation (JSR-380) | P1 | DX improvement |
| RFC-0015 | Observability | P2 | Production operations |

---

## 7. DX Quality Checklist

For every feature, verify:

- [ ] **Zero-config start:** Works out of the box
- [ ] **Full control available:** Power users can customize everything
- [ ] **Standard patterns:** Uses familiar Java idioms
- [ ] **No reinvention:** Leverages existing libraries
- [ ] **Explicit where it matters:** Business logic is visible
- [ ] **Automatic where boring:** Infrastructure is hidden
- [ ] **Good errors:** Messages explain what went wrong and how to fix
- [ ] **Testable:** Can be unit tested in isolation
- [ ] **Documented:** Has Javadoc and usage examples

---

## 8. Concrete Next Steps

### Step 1: Add SLF4J to axiom-core (Today)
```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.9</version>
</dependency>
```

Replace all `System.err.println` with proper logging.

### Step 2: Write RFC-0012 (Logging)
Define how logging integrates with:
- Request correlation (MDC)
- Lifecycle events
- Error handling

### Step 3: Complete RFC-0010 (Testing)
Implement MockContext, AppTester, TestClient.

### Step 4: Enhance RFC-0011
Add missing details:
- Configuration properties
- HikariCP default
- Flyway integration
- Test support

### Step 5: Start RFC-0011 Implementation
Build persistence layer with enhanced DX.

---

## 9. Success Metrics

Axiom has excellent DX when:

1. **5-minute quickstart:** New user can have working app in 5 minutes
2. **Zero boilerplate:** No repetitive code required
3. **Obvious errors:** When something fails, user knows why
4. **Standard patterns:** Experienced Java devs feel at home
5. **Production-ready defaults:** Works correctly without tuning
6. **Full control:** Power users can customize everything

---

## Summary

RFC-0011's design is **GOOD** — the `@Transactional` approach is correct infrastructure magic that Java developers expect. Before implementing it, we need:

1. **SLF4J logging** — replace System.err
2. **Testing utilities** — complete RFC-0010
3. **Configuration system** — external properties support

The key principle: **Don't reinvent. Integrate existing standards well.**

Axiom should feel like a lighter, more explicit Spring Boot — same productivity, more control, less magic in business logic.
