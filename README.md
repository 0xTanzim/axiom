# Axiom

> **DX-first, functional Java web framework for modern JVM development.**

[![Java 25](https://img.shields.io/badge/Java-25-blue.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.0xtanzim/axiom.svg)](https://search.maven.org/artifact/io.github.0xtanzim/axiom)

---

## What is Axiom?

Axiom is a **developer experience first** web framework for Java 25+.

It brings the simplicity of Express/Hono to the JVM — without reflection,
classpath scanning, or magic. Just code.

```java
import io.axiom.core.app.Axiom;
import io.axiom.core.routing.Router;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        Router router = new Router();

        router.get("/", c -> c.text("Hello, Axiom!"));

        router.get("/users/:id", c -> {
            String id = c.param("id");
            c.json(Map.of("id", id, "name", "User " + id));
        });

        router.post("/users", c -> {
            User user = c.body(User.class);
            c.status(201);
            c.json(user);
        });

        Axiom.start(router, 8080);
    }
}

record User(String name, String email) {}
```

**No annotations. No reflection. No magic.**

---

## Quick Start

### Option 1: Generate a New Project (Recommended)

**Linux/macOS:**
```bash
mvn archetype:generate \
  -DarchetypeGroupId=io.github.0xtanzim \
  -DarchetypeArtifactId=axiom-quickstart \
  -DarchetypeVersion=0.1.4 \
  -DgroupId=com.example \
  -DartifactId=my-app \
  -Dversion=1.0.0 \
  -DinteractiveMode=false
```

**Windows (CMD):**
```cmd
mvn archetype:generate ^
  -DarchetypeGroupId=io.github.0xtanzim ^
  -DarchetypeArtifactId=axiom-quickstart ^
  -DarchetypeVersion=0.1.4 ^
  -DgroupId=com.example ^
  -DartifactId=my-app ^
  -Dversion=1.0.0 ^
  -DinteractiveMode=false
```

**Windows (PowerShell):**
```powershell
mvn archetype:generate `
  -DarchetypeGroupId=io.github.0xtanzim `
  -DarchetypeArtifactId=axiom-quickstart `
  -DarchetypeVersion=0.1.4 `
  -DgroupId=com.example `
  -DartifactId=my-app `
  -Dversion=1.0.0 `
  -DinteractiveMode=false
```

This creates a ready-to-run project:
```
my-app/
├── pom.xml
└── src/main/java/com/example/App.java
```

Run it:
```bash
cd my-app
mvn compile exec:java
# 🚀 Server running at http://localhost:8080
```

### Option 2: Add to Existing Project

**Maven:**
```xml
<dependency>
    <groupId>io.github.0xtanzim</groupId>
    <artifactId>axiom</artifactId>
    <version>0.1.4</version>
    <type>pom</type>
</dependency>

<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```

**Gradle:**
```kotlin
implementation("io.github.0xtanzim:axiom:0.1.4")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
```

### Create Your App

```java
// src/main/java/App.java
import io.axiom.core.app.Axiom;
import io.axiom.core.routing.Router;

public class App {
    public static void main(String[] args) {
        Router router = new Router();
        router.get("/", c -> c.text("Hello, Axiom!"));

        Axiom.start(router, 8080);
        System.out.println("🚀 Server running at http://localhost:8080");
    }
}
```

### Run the Server

**Maven:**
```bash
mvn compile exec:java -Dexec.mainClass="App"
```

**Gradle:**
```bash
./gradlew run
```

### Test It

**Linux/macOS:**
```bash
curl http://localhost:8080
# Hello, Axiom!
```

**Windows (PowerShell):**
```powershell
Invoke-WebRequest -Uri http://localhost:8080
# Or use: curl http://localhost:8080 (if curl alias is available)
```

**Windows (CMD) - requires curl.exe (Windows 10+):**
```cmd
curl http://localhost:8080
# Hello, Axiom!
```

That's it. Your server is running.

---

## Running the Server

Axiom provides multiple ways to start your server:

### Simple Start (Recommended)

```java
// One-liner: router + port → running server
Axiom.start(router, 8080);
```

### With Host Binding

```java
// Bind to specific host
Axiom.start(router, "127.0.1.4", 8080);  // localhost only
Axiom.start(router, "0.1.4.0", 8080);    // all interfaces
```

### Full Control

```java
import io.axiom.core.app.*;
import io.axiom.core.routing.Router;
import java.util.Map;

App app = Axiom.create();

// Add middleware
app.use((ctx, next) -> {
    System.out.println(ctx.method() + " " + ctx.path());
    next.run();
});

// Lifecycle hooks
app.onStart(() -> System.out.println("Connecting to database..."));
app.onReady(() -> System.out.println("🚀 Server ready on port " + app.port()));
app.onShutdown(() -> System.out.println("Cleaning up..."));

// Error handling
app.onError((ctx, e) -> {
    ctx.status(500);
    ctx.json(Map.of("error", e.getMessage()));
});

// Routes
app.route(router);

// Start (blocks until shutdown)
app.listen(8080);
```

### Server Configuration

```java
import io.axiom.core.server.ServerConfig;
import java.time.Duration;

App app = Axiom.create();
app.route(router);

// Custom configuration
app.listen(ServerConfig.builder()
    .host("0.1.4.0")
    .port(8080)
    .readTimeout(Duration.ofSeconds(30))
    .writeTimeout(Duration.ofSeconds(30))
    .shutdownTimeout(Duration.ofSeconds(30))
    .virtualThreads(true)  // default: true
    .build());
```

### Using Config File

```properties
# application.properties
server.host=0.1.4.0
server.port=8080
```

```java
import io.axiom.config.Config;

Config.ServerConfig server = Config.server();
app.listen(server.host(), server.port());
```

---

## Core Concepts

### Routing

```java
Router router = new Router();

// Methods
router.get("/users", c -> { /* ... */ });
router.post("/users", c -> { /* ... */ });
router.put("/users/:id", c -> { /* ... */ });
router.delete("/users/:id", c -> { /* ... */ });

// Path parameters
router.get("/users/:id", c -> {
    String id = c.param("id");
});

// Query parameters
router.get("/search", c -> {
    String q = c.query("q");
    int page = c.query("page", 1);
});

// Route groups
router.group("/api", api -> {
    api.get("/users", userHandler);
    api.get("/posts", postHandler);
});
```

### Context

```java
router.post("/users", c -> {
    // Request
    String method = c.method();        // "POST"
    String path = c.path();            // "/users"
    String id = c.param("id");         // path param
    String q = c.query("q");           // query param
    String auth = c.header("Authorization");
    User body = c.body(User.class);    // JSON body

    // Response
    c.status(201);
    c.header("X-Custom", "value");
    c.json(user);                      // JSON response
    c.text("OK");                      // text response
    c.html("<h1>Hi</h1>");             // HTML response
});
```

### Middleware

```java
// Logging middleware
app.use((ctx, next) -> {
    long start = System.nanoTime();
    next.run();
    long ms = (System.nanoTime() - start) / 1_000_000;
    System.out.println(ctx.method() + " " + ctx.path() + " - " + ms + "ms");
});

// Auth middleware
app.use((ctx, next) -> {
    String token = ctx.header("Authorization");
    if (token == null) {
        ctx.status(401);
        ctx.json(Map.of("error", "Unauthorized"));
        return;
    }
    next.run();
});
```

---

## Requirements

- **Java 25** (LTS) — required for virtual threads and modern features

---

## Why Axiom?

| | Spring | Javalin | **Axiom** |
|---|--------|---------|-----------|
| Annotations | Heavy | Minimal | **None** |
| Reflection | Heavy | Some | **None** |
| Virtual Threads | Optional | Optional | **Default** |
| Core Size | ~50MB | ~1MB | **<100KB** |
| Learning Curve | High | Low | **Low** |

---

## Documentation

📚 **Full documentation:** https://0xtanzim.github.io/axiom/

| Guide | Description |
|-------|-------------|
| [Getting Started](https://0xtanzim.github.io/axiom/docs/getting-started) | Installation and first app |
| [Routing](https://0xtanzim.github.io/axiom/docs/routing) | Routes, groups, parameters |
| [Middleware](https://0xtanzim.github.io/axiom/docs/middleware) | Request/response pipeline |
| [Context](https://0xtanzim.github.io/axiom/docs/context) | Request and response API |
| [Configuration](https://0xtanzim.github.io/axiom/docs/config) | Config files, env vars |
| [Lifecycle](https://0xtanzim.github.io/axiom/docs/lifecycle) | Startup, shutdown hooks |
| [Persistence](https://0xtanzim.github.io/axiom/docs/persistence) | Database integration |

---

## Project Structure

```
axiom/
├── axiom-core/                 # Core framework (routing, config, DI, server)
├── axiom-persistence/          # Database integration
├── axiom-persistence-processor/# Compile-time annotation processor
└── axiom-framework/            # Published as "axiom" (THE ONE dependency)
```

Users add **one dependency**: `io.github.0xtanzim:axiom`

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

---

## License

MIT License — See [LICENSE](LICENSE) for details.

---

## Acknowledgments

Inspired by:
- [NextRush](https://github.com/0xTanzim/nextRush) — DX and simplicity
- [Hono](https://hono.dev) — DX and simplicity
- [Express](https://expressjs.com) — Middleware model
- [Javalin](https://javalin.io) — Java simplicity

---

*Built for developers who believe Java can be simple.*
