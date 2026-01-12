# Architecture Decision Records (ADR)

**Version:** 0.1.0-draft
**Last Updated:** 2026-01-12

---

## Index

| ID | Title | Status | Date |
|----|-------|--------|------|
| ADR-001 | Trie-Based Routing | Accepted | 2026-01-12 |
| ADR-002 | Functional Middleware Composition | Accepted | 2026-01-12 |
| ADR-003 | Virtual Threads as Default | Accepted | 2026-01-12 |
| ADR-004 | Build Tool Agnostic | Accepted | 2026-01-12 |
| ADR-005 | No Annotations in Core | Accepted | 2026-01-12 |
| ADR-006 | ScopedValue over ThreadLocal | Accepted | 2026-01-12 |
| ADR-007 | Sealed Interfaces for Type Safety | Accepted | 2026-01-12 |
| ADR-008 | Runtime Adapter Pattern | Accepted | 2026-01-12 |
| ADR-009 | Response Commitment Model | Accepted | 2026-01-12 |
| ADR-010 | JSON Codec as Separate Module | Accepted | 2026-01-12 |

---

## ADR-001: Trie-Based Routing

### Status
Accepted

### Context
Routing is on the hot path of every request. Common approaches include:
- Linear scan (O(n) routes)
- Regex matching (expensive per request)
- Trie/radix tree (O(path depth))

### Decision
Use a method-scoped trie structure for route matching.

```java
GET
 └─ users
     ├─ :id → Handler
     └─ me  → Handler (static wins)
```

### Consequences
- ✅ O(depth) matching, not O(routes)
- ✅ No regex compilation per request
- ✅ Predictable precedence (static > param > wildcard)
- ✅ Low memory overhead
- ⚠️ Complex wildcard patterns not supported (intentional)

### References
- RFC-0003: Routing Matcher Algorithm

---

## ADR-002: Functional Middleware Composition

### Status
Accepted

### Context
Middleware systems typically use:
- Filter chains (servlet style)
- Interceptor pattern
- Functional composition (Hono/Express)

### Decision
Middleware is represented as function composition internally:

```java
// Internal representation
interface Middleware {
    Handler apply(Handler next);
}

// Public DX
app.use((c, next) -> {
    log(c.path());
    next.run();
});
```

### Consequences
- ✅ Clean user-facing API
- ✅ Single composed handler at runtime
- ✅ No per-request chain building
- ✅ Explicit control flow
- ⚠️ Order matters (registration order = execution order)

### References
- RFC-0004: Middleware Pipeline Internals

---

## ADR-003: Virtual Threads as Default

### Status
Accepted

### Context
Java 21+ provides virtual threads (Project Loom).
Options:
1. Platform thread pool (traditional)
2. Virtual threads (new)
3. Reactive streams (RxJava, Reactor)

### Decision
Virtual threads are the default execution model.

```java
// Each request runs on its own virtual thread
Executors.newVirtualThreadPerTaskExecutor();
```

### Consequences
- ✅ Blocking syntax, async scalability
- ✅ Familiar to Java developers
- ✅ No callback hell
- ✅ Simple debugging (stack traces work)
- ⚠️ Requires Java 21+
- ⚠️ Pinning issues in Java 21 (fixed in 25)

### References
- RFC-0001: Execution Model

---

## ADR-004: Build Tool Agnostic

### Status
Accepted

### Context
Frameworks often couple to build tools via:
- Maven plugins
- Gradle plugins
- Code generation
- Annotation processors

### Decision
Framework ships as plain JARs only. No plugins, no processors.

```xml
<!-- Maven: just a dependency -->
<dependency>
    <groupId>io.axiom</groupId>
    <artifactId>axiom-core</artifactId>
</dependency>
```

### Consequences
- ✅ Works with any build tool
- ✅ No plugin version coupling
- ✅ No magic build steps
- ✅ Simple mental model
- ⚠️ No dev mode hot reload (by design)
- ⚠️ No scaffolding CLI

### References
- RFC-0006: Build Tool & Kotlin Compatibility

---

## ADR-005: No Annotations in Core

### Status
Accepted

### Context
Java frameworks commonly use annotations for:
- Route mapping (@GET, @Path)
- Dependency injection (@Inject)
- Lifecycle (@PostConstruct)
- Configuration (@Value)

### Decision
Core framework uses ZERO annotations. Routes are code.

```java
// Yes: explicit code
router.get("/users/:id", c -> c.json(findUser(c.param("id"))));

// No: annotation magic
@GET @Path("/users/{id}")
public User getUser(@PathParam("id") String id) { ... }
```

### Consequences
- ✅ No reflection in hot path
- ✅ No annotation processing
- ✅ IDE navigation works naturally
- ✅ Easy to understand execution flow
- ⚠️ More verbose for large APIs
- ⚠️ No auto-discovery

### References
- RFC-0001: Core Design Principles

---

## ADR-006: ScopedValue over ThreadLocal

### Status
Accepted

### Context
Request-scoped data needs storage. Options:
1. ThreadLocal (classic, leaky)
2. ScopedValue (Java 21+, structured)
3. Context parameter passing (explicit)

### Decision
Use ScopedValue internally, expose via Context API.

```java
// Internal
static final ScopedValue<RequestContext> REQUEST = ScopedValue.newInstance();

// Public API
c.get("user", User.class);  // backed by ScopedValue
```

### Consequences
- ✅ No ThreadLocal cleanup issues
- ✅ Structured lifetime
- ✅ Virtual thread friendly
- ✅ Clear ownership
- ⚠️ Java 21+ only
- ⚠️ Cannot mutate after binding

### References
- Java 21 JEP 446: Scoped Values

---

## ADR-007: Sealed Interfaces for Type Safety

### Status
Accepted

### Context
Java 17+ provides sealed interfaces for exhaustive pattern matching.

### Decision
Use sealed interfaces for controlled extension points.

```java
public sealed interface Segment
    permits StaticSegment, ParamSegment, WildcardSegment {

    record StaticSegment(String value) implements Segment {}
    record ParamSegment(String name) implements Segment {}
    record WildcardSegment() implements Segment {}
}
```

### Consequences
- ✅ Compile-time exhaustiveness checks
- ✅ Clear extension points
- ✅ Pattern matching support
- ✅ Self-documenting hierarchy
- ⚠️ Cannot extend outside module

### References
- Java 17 JEP 409: Sealed Classes

---

## ADR-008: Runtime Adapter Pattern

### Status
Accepted

### Context
Multiple HTTP servers exist (JDK, Netty, Undertow).
Options:
1. Hardcode one server
2. Abstraction layer with adapters
3. Separate framework per server

### Decision
Core defines Server interface, adapters implement it.

```java
public interface Server {
    void start(ServerConfig config);
    void stop();
    void onRequest(Handler handler);
}
```

### Consequences
- ✅ Framework-agnostic core
- ✅ Users choose runtime
- ✅ Performance optimizations per adapter
- ✅ Future server support easy
- ⚠️ Lowest common denominator API
- ⚠️ Feature parity challenges

### References
- RFC-0001: Runtime Architecture

---

## ADR-009: Response Commitment Model

### Status
Accepted

### Context
When is a response "final"? Options:
1. First write commits (strict)
2. Explicit commit method
3. End of handler commits

### Decision
Writing response body commits the response. No more writes allowed.

```java
c.text("Hello");  // Response committed
c.text("World");  // Throws ResponseCommittedException
```

### Consequences
- ✅ Prevents duplicate responses
- ✅ Clear state machine
- ✅ Middleware can short-circuit safely
- ✅ Predictable behavior
- ⚠️ No streaming response modification
- ⚠️ Headers must be set before body

### References
- RFC-0005: Response Commitment Rule

---

## ADR-010: JSON Codec as Separate Module

### Status
Accepted

### Context
JSON is common but not universal. Options:
1. Bundle JSON in core (bloat)
2. Require JSON library (coupling)
3. Optional module (flexibility)

### Decision
JSON codecs are separate optional modules.

```xml
<!-- Choose one -->
<dependency>
    <artifactId>axiom-json-jackson</artifactId>
</dependency>
<!-- OR -->
<dependency>
    <artifactId>axiom-json-gson</artifactId>
</dependency>
```

### Consequences
- ✅ Core stays minimal
- ✅ User chooses JSON library
- ✅ No dependency conflicts
- ✅ Can use without JSON
- ⚠️ Extra dependency for common case
- ⚠️ No default JSON out of box

### References
- RFC-0006: Module Strategy

---

## Pending Decisions

| ID | Title | Status |
|----|-------|--------|
| ADR-011 | Configuration System | Pending RFC |
| ADR-012 | Logging Strategy | Pending RFC |
| ADR-013 | WebSocket Support | Pending RFC |
| ADR-014 | HTTP/2 Strategy | Pending RFC |
| ADR-015 | Validation Module | Pending RFC |

---

*ADRs document significant architectural decisions and their rationale.*
