# Axiom

> DX-first, functional Java web framework for modern JVM development.

[![Java 21+](https://img.shields.io/badge/Java-21%2B-blue.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![Status](https://img.shields.io/badge/Status-Architecture%20Phase-orange.svg)](#status)

---

## Vision

Axiom is a **developer experience first** web framework for Java 21+.

It brings the simplicity of Express/Hono to the JVM — without reflection,
annotations, or magic. Just code.

```java
Router router = new Router();

router.get("/health", c -> c.text("OK"));

router.get("/users/:id", c -> {
    User user = userService.find(c.param("id"));
    c.json(user);
});

router.post("/users", c -> {
    UserCreate req = c.body(UserCreate.class);
    User created = userService.create(req);
    c.status(201);
    c.json(created);
});

App app = new App();
app.use(loggingMiddleware);
app.route(router);
app.listen(8080);
```

**No annotations. No reflection. No magic.**

---

## Core Principles

| Principle | Description |
|-----------|-------------|
| **DX First** | Developer experience above all else |
| **Explicit** | No hidden behavior, clear execution flow |
| **Functional** | Composition over inheritance |
| **Fast** | Zero reflection in hot paths |
| **Modern** | Java 21+, virtual threads, sealed types |
| **Flexible** | Runtime adapter architecture |

---

## Status

🚧 **Architecture Phase** — Not ready for use.

We're designing the framework before writing code. All design decisions
are documented in RFCs under `/draft`.

### Roadmap

| Phase | Status | Description |
|-------|--------|-------------|
| 0. Foundation | ✅ In Progress | RFCs, architecture docs |
| 1. Core Engine | ⏳ Pending | Handler, Router, Middleware |
| 2. HTTP Layer | ⏳ Pending | HTTP types, Context |
| 3. JDK Runtime | ⏳ Pending | First working server |
| 4. Testing | ⏳ Pending | Test utilities |
| 5. JSON Codecs | ⏳ Pending | Jackson, Gson |
| 6. Documentation | ⏳ Pending | User guides |
| 7. Performance | ⏳ Pending | Benchmarks |
| 8. Alpha Release | ⏳ Pending | 0.1.0-alpha |

See [ROADMAP.md](docs/plan/ROADMAP.md) for details.

---

## Why Another Framework?

Java web frameworks today are either:

1. **Heavy** — Spring Boot with 50MB of dependencies
2. **Magical** — Annotation scanning, reflection, hidden lifecycles
3. **Complex** — Reactive streams leaking into user code
4. **Outdated** — Not leveraging Java 21+ features

Axiom is different:

| | Spring | Javalin | Vert.x | **Axiom** |
|---|--------|---------|--------|-----------|
| Annotations | Heavy | Minimal | None | **None** |
| Reflection | Heavy | Some | Minimal | **None** |
| Virtual Threads | Optional | Optional | No | **Default** |
| Core Size | ~50MB | ~1MB | ~5MB | **<100KB** |
| Learning Curve | High | Low | Medium | **Low** |

---

## Design

### Architecture

```
┌────────────────────────────────────────────────────┐
│                    Your App                         │
└────────────────────────┬───────────────────────────┘
                         │
┌────────────────────────┴───────────────────────────┐
│              Runtime Adapter (choose one)           │
│    axiom-runtime-jdk │ axiom-runtime-netty │ ...   │
└────────────────────────┬───────────────────────────┘
                         │
┌────────────────────────┴───────────────────────────┐
│                    axiom-http                       │
│            HTTP types, body parsing                 │
└────────────────────────┬───────────────────────────┘
                         │
┌────────────────────────┴───────────────────────────┐
│                    axiom-core                       │
│      Handler, Router, Middleware, Lifecycle         │
└────────────────────────────────────────────────────┘
```

### Key Abstractions

**Handler** — The fundamental unit of work:
```java
@FunctionalInterface
public interface Handler {
    void handle(Context c) throws Exception;
}
```

**Context** — Request + Response in one place:
```java
// Request (immutable)
c.method()           // "GET"
c.path()             // "/users/123"
c.param("id")        // "123"
c.query("page")      // "1"
c.body(User.class)   // parsed body

// Response (mutable)
c.status(201)
c.header("X-Custom", "value")
c.json(response)
```

**Router** — Express-style routing:
```java
Router router = new Router();
router.get("/users", listHandler);
router.get("/users/:id", getHandler);
router.post("/users", createHandler);
router.group("/admin", admin -> {
    admin.get("/stats", statsHandler);
});
```

**Middleware** — Onion-style composition:
```java
app.use((c, next) -> {
    long start = System.nanoTime();
    next.run();
    long duration = System.nanoTime() - start;
    System.out.println("Request took: " + duration + "ns");
});
```

---

## Modules

**What users see:** ONE dependency

```xml
<dependency>
    <groupId>io.axiom</groupId>
    <artifactId>axiom</artifactId>
    <version>0.1.0</version>
</dependency>
```

**What's inside (internal):**

| Module | Purpose |
|--------|---------|
| `axiom-core` | Core primitives (zero external deps) |
| `axiom-config` | Configuration |
| `axiom-di` | Dependency Injection |
| `axiom-validation` | Input validation |
| `axiom-server` | HTTP server |
| `axiom-persistence` | Database |

> Users don't need to know about internal modules. Just add `axiom`.

---

## Requirements

- **Java 21** minimum (virtual threads)
- **Java 25 LTS** recommended (full feature support)
- No build tool requirements (works with Maven, Gradle, any)

---

## Installation

> ⚠️ Not published to Maven Central yet — in development.

**Maven:**
```xml
<dependency>
    <groupId>io.axiom</groupId>
    <artifactId>axiom</artifactId>
    <version>0.1.0</version>
</dependency>
```

**Gradle:**
```kotlin
implementation("io.axiom:axiom:0.1.0")
```

That's it. ONE dependency. Full framework.

---

## Quick Start

> ⚠️ Example only — not implemented yet.

```java
import io.axiom.core.*;
import io.axiom.runtime.jdk.*;

public class Main {
    public static void main(String[] args) {
        Router router = new Router();

        router.get("/", c -> c.text("Hello, Axiom!"));

        router.get("/users/:id", c -> {
            String id = c.param("id");
            c.json(Map.of("id", id, "name", "User " + id));
        });

        App app = new JdkApp();
        app.route(router);
        app.listen(8080);

        System.out.println("Server running at http://localhost:8080");
    }
}
```

---

## Documentation

| Document | Description |
|----------|-------------|
| [Architecture](docs/architecture/ARCHITECTURE.md) | Technical design |
| [Modules](docs/architecture/MODULES.md) | Project structure |
| [Decisions](docs/architecture/DECISIONS.md) | ADRs |
| [Roadmap](docs/plan/ROADMAP.md) | Implementation plan |
| [Edge Cases](docs/architecture/EDGE_CASES.md) | Safety considerations |

### RFCs

| RFC | Title | Status |
|-----|-------|--------|
| [RFC-0001](draft/RFC_0001.md) | Core Framework Design | Draft |
| [RFC-0002](draft/RFC_0002.md) | Routing & Composition | Draft |
| [RFC-0003](draft/RFC_0003.md) | Routing Matcher Algorithm | Draft |
| [RFC-0004](draft/RFC_0004.md) | Middleware Pipeline | Draft |
| [RFC-0005](draft/RFC_0005.md) | DX Philosophy | Draft |
| [RFC-0006](draft/RFC_0006.md) | Build Tool Strategy | Draft |

---

## Comparison

### vs Spring Boot

```java
// Spring Boot
@RestController
@RequestMapping("/users")
public class UserController {
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable String id) {
        return ResponseEntity.ok(userService.find(id));
    }
}

// Axiom
router.get("/users/:id", c -> c.json(userService.find(c.param("id"))));
```

### vs Javalin

```java
// Javalin
app.get("/users/{id}", ctx -> {
    ctx.json(userService.find(ctx.pathParam("id")));
});

// Axiom (nearly identical — that's the point!)
router.get("/users/:id", c -> c.json(userService.find(c.param("id"))));
```

---

## Philosophy

> "Use what fits your brain."

Axiom supports multiple styles without forcing one:

**Middleware style (Express/Hono):**
```java
app.use((c, next) -> {
    if (!isAuthenticated(c)) {
        c.status(401);
        c.text("Unauthorized");
        return;
    }
    next.run();
});
```

**Hook style (Javalin):**
```java
app.before(c -> {
    if (!isAuthenticated(c)) {
        c.status(401);
        c.text("Unauthorized");
    }
});
```

Both are first-class. Both work. Choose what fits your brain.

---

## Contributing

Axiom is in early design phase. Contributions welcome:

1. **Review RFCs** — Give feedback on design
2. **Propose RFCs** — Suggest new features
3. **Documentation** — Help improve docs
4. **Future: Code** — Once design is stable

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

---

## License

Apache License 2.0 — See [LICENSE](LICENSE) for details.

---

## Acknowledgments

Inspired by:
- [Hono](https://hono.dev) — DX and simplicity
- [Express](https://expressjs.com) — Middleware model
- [Javalin](https://javalin.io) — Java simplicity
- Project Loom — Virtual threads

---

*Built for developers who believe Java can be simple.*
