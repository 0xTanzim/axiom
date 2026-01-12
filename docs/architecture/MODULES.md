# Axiom Module Structure

**Version:** 0.1.0-draft
**Last Updated:** 2026-01-12

---

## Overview

Axiom follows a strict multi-module architecture to ensure clean separation
between core abstractions, HTTP types, and runtime implementations.

---

## Project Layout

```
axiom/
│
├── build.gradle.kts              # Root build configuration
├── settings.gradle.kts           # Multi-module settings
├── gradle.properties             # Shared build properties
├── gradle/
│   └── libs.versions.toml        # Version catalog
│
├── LICENSE                       # Apache 2.0
├── README.md                     # Project overview
├── CHANGELOG.md                  # Version history
├── CONTRIBUTING.md               # Contribution guide
├── SECURITY.md                   # Security policy
│
├── draft/                        # RFC documents (design source of truth)
│   ├── RFC_0001.md               # Core Framework Design
│   ├── RFC_0002.md               # Routing & Composition
│   ├── RFC_0003.md               # Routing Matcher Algorithm
│   ├── RFC_0004.md               # Middleware Pipeline
│   ├── RFC_0005.md               # DX Philosophy
│   ├── RFC_0006.md               # Build Tool Strategy
│   ├── RFC_0007.md               # [NEEDED] Lifecycle Management
│   ├── RFC_0008.md               # [NEEDED] Error Handling
│   ├── RFC_0009.md               # [NEEDED] Runtime Adapter Contract
│   └── RFC_0010.md               # [NEEDED] Testing Utilities
│
├── docs/                         # Documentation
│   ├── architecture/             # Architecture docs
│   │   ├── ARCHITECTURE.md       # Main architecture document
│   │   ├── MODULES.md            # This file
│   │   ├── DECISIONS.md          # Architecture Decision Records
│   │   └── DIAGRAMS.md           # Visual diagrams
│   │
│   ├── plan/                     # Planning documents
│   │   ├── ROADMAP.md            # Implementation roadmap
│   │   └── MILESTONES.md         # Release milestones
│   │
│   ├── todos/                    # Task tracking
│   │   ├── PHASE1.md             # Phase 1 tasks
│   │   └── BLOCKERS.md           # Known blockers
│   │
│   └── guide/                    # User guide (future VitePress)
│       ├── getting-started.md
│       ├── routing.md
│       ├── middleware.md
│       └── deployment.md
│
├── axiom-bom/                    # Bill of Materials
│   └── build.gradle.kts
│
├── axiom-core/                   # TIER 1: Core abstractions
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── io/axiom/core/
│       │   │       ├── handler/
│       │   │       │   ├── Handler.java
│       │   │       │   └── HandlerChain.java
│       │   │       ├── context/
│       │   │       │   ├── Context.java
│       │   │       │   ├── RequestData.java
│       │   │       │   └── ResponseWriter.java
│       │   │       ├── routing/
│       │   │       │   ├── Router.java
│       │   │       │   ├── Route.java
│       │   │       │   ├── RouteMatch.java
│       │   │       │   ├── Segment.java
│       │   │       │   └── internal/
│       │   │       │       └── RouteTrie.java
│       │   │       ├── middleware/
│       │   │       │   ├── Middleware.java
│       │   │       │   ├── MiddlewareHandler.java
│       │   │       │   └── Next.java
│       │   │       ├── lifecycle/
│       │   │       │   ├── Lifecycle.java
│       │   │       │   ├── LifecycleHook.java
│       │   │       │   └── LifecycleAware.java
│       │   │       ├── error/
│       │   │       │   ├── AxiomException.java
│       │   │       │   ├── RouteNotFoundException.java
│       │   │       │   ├── MethodNotAllowedException.java
│       │   │       │   ├── BodyParseException.java
│       │   │       │   └── ResponseCommittedException.java
│       │   │       └── app/
│       │   │           └── App.java
│       │   └── module-info.java
│       └── test/
│           └── java/
│               └── io/axiom/core/
│                   └── ...
│
├── axiom-http/                   # TIER 2: HTTP abstractions
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── io/axiom/http/
│       │   │       ├── HttpMethod.java
│       │   │       ├── HttpStatus.java
│       │   │       ├── Headers.java
│       │   │       ├── ContentType.java
│       │   │       ├── body/
│       │   │       │   ├── BodyParser.java
│       │   │       │   ├── BodyCodec.java
│       │   │       │   └── JsonCodec.java
│       │   │       └── context/
│       │   │           └── HttpContext.java
│       │   └── module-info.java
│       └── test/
│           └── ...
│
├── axiom-runtime-jdk/            # TIER 3a: JDK HttpServer adapter
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── io/axiom/runtime/jdk/
│       │   │       ├── JdkServer.java
│       │   │       ├── JdkContext.java
│       │   │       └── JdkApp.java
│       │   └── module-info.java
│       └── test/
│           └── ...
│
├── axiom-runtime-netty/          # TIER 3b: Netty adapter
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── io/axiom/runtime/netty/
│       │   │       ├── NettyServer.java
│       │   │       ├── NettyContext.java
│       │   │       └── NettyApp.java
│       │   └── module-info.java
│       └── test/
│           └── ...
│
├── axiom-json-jackson/           # TIER 4: Jackson JSON codec
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── io/axiom/json/jackson/
│       │   │       └── JacksonCodec.java
│       │   └── module-info.java
│       └── test/
│           └── ...
│
├── axiom-json-gson/              # TIER 4: Gson JSON codec
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── io/axiom/json/gson/
│       │   │       └── GsonCodec.java
│       │   └── module-info.java
│       └── test/
│           └── ...
│
├── axiom-test/                   # Testing utilities
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── io/axiom/test/
│       │   │       ├── MockContext.java
│       │   │       ├── TestRouter.java
│       │   │       ├── TestApp.java
│       │   │       └── Assertions.java
│       │   └── module-info.java
│       └── test/
│           └── ...
│
├── examples/                     # Example projects
│   ├── hello-world/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/
│   │       └── demo/HelloWorld.java
│   ├── rest-api/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/
│   │       └── demo/
│   │           ├── Main.java
│   │           └── routes/
│   │               └── UserRoutes.java
│   └── middleware-demo/
│       ├── build.gradle.kts
│       └── src/main/java/
│           └── demo/MiddlewareDemo.java
│
└── benchmarks/                   # Performance benchmarks
    ├── build.gradle.kts
    └── src/jmh/java/
        └── io/axiom/bench/
            ├── RoutingBenchmark.java
            └── HandlerBenchmark.java
```

---

## Module Descriptions

### Core Modules (Required)

| Module | Description | Dependencies |
|--------|-------------|--------------|
| `axiom-core` | Core abstractions: Handler, Context, Router, Middleware | JDK only |
| `axiom-http` | HTTP types: Method, Status, Headers, Body parsing | axiom-core |

### Runtime Modules (Choose One)

| Module | Description | Dependencies |
|--------|-------------|--------------|
| `axiom-runtime-jdk` | JDK HttpServer adapter (default) | axiom-core, axiom-http |
| `axiom-runtime-netty` | Netty high-performance adapter | axiom-core, axiom-http, netty |
| `axiom-runtime-undertow` | Undertow adapter (future) | axiom-core, axiom-http, undertow |

### Extension Modules (Optional)

| Module | Description | Dependencies |
|--------|-------------|--------------|
| `axiom-json-jackson` | Jackson JSON codec | axiom-core, jackson |
| `axiom-json-gson` | Gson JSON codec | axiom-core, gson |
| `axiom-test` | Testing utilities | axiom-core |

### Support Modules

| Module | Description | Purpose |
|--------|-------------|---------|
| `axiom-bom` | Bill of Materials | Version alignment |

---

## Maven Coordinates

```xml
<!-- Core (required) -->
<dependency>
    <groupId>io.axiom</groupId>
    <artifactId>axiom-core</artifactId>
    <version>${axiom.version}</version>
</dependency>

<!-- HTTP (required for web) -->
<dependency>
    <groupId>io.axiom</groupId>
    <artifactId>axiom-http</artifactId>
    <version>${axiom.version}</version>
</dependency>

<!-- Runtime (choose one) -->
<dependency>
    <groupId>io.axiom</groupId>
    <artifactId>axiom-runtime-jdk</artifactId>
    <version>${axiom.version}</version>
</dependency>

<!-- JSON (optional) -->
<dependency>
    <groupId>io.axiom</groupId>
    <artifactId>axiom-json-jackson</artifactId>
    <version>${axiom.version}</version>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>io.axiom</groupId>
    <artifactId>axiom-test</artifactId>
    <version>${axiom.version}</version>
    <scope>test</scope>
</dependency>
```

---

## Gradle Coordinates

```kotlin
// Core (required)
implementation("io.axiom:axiom-core:${axiomVersion}")

// HTTP (required for web)
implementation("io.axiom:axiom-http:${axiomVersion}")

// Runtime (choose one)
implementation("io.axiom:axiom-runtime-jdk:${axiomVersion}")

// JSON (optional)
implementation("io.axiom:axiom-json-jackson:${axiomVersion}")

// Testing
testImplementation("io.axiom:axiom-test:${axiomVersion}")
```

---

## Dependency Graph

```
                    ┌─────────────────┐
                    │   User App      │
                    └────────┬────────┘
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
          ▼                  ▼                  ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ axiom-runtime-* │ │ axiom-json-*    │ │ axiom-test      │
└────────┬────────┘ └────────┬────────┘ └────────┬────────┘
         │                   │                   │
         └─────────┬─────────┴───────────────────┘
                   │
                   ▼
          ┌─────────────────┐
          │   axiom-http    │
          └────────┬────────┘
                   │
                   ▼
          ┌─────────────────┐
          │   axiom-core    │
          └────────┬────────┘
                   │
                   ▼
              JDK stdlib
```

---

## JPMS Module Names

| Artifact | Module Name |
|----------|-------------|
| axiom-core | `io.axiom.core` |
| axiom-http | `io.axiom.http` |
| axiom-runtime-jdk | `io.axiom.runtime.jdk` |
| axiom-runtime-netty | `io.axiom.runtime.netty` |
| axiom-json-jackson | `io.axiom.json.jackson` |
| axiom-json-gson | `io.axiom.json.gson` |
| axiom-test | `io.axiom.test` |

---

## Build Configuration

### Root `settings.gradle.kts`

```kotlin
rootProject.name = "axiom"

include(
    "axiom-bom",
    "axiom-core",
    "axiom-http",
    "axiom-runtime-jdk",
    "axiom-runtime-netty",
    "axiom-json-jackson",
    "axiom-json-gson",
    "axiom-test"
)

// Examples (not published)
include(
    "examples:hello-world",
    "examples:rest-api",
    "examples:middleware-demo"
)

// Benchmarks (not published)
include("benchmarks")
```

### Root `build.gradle.kts`

```kotlin
plugins {
    java
    `maven-publish`
    signing
}

allprojects {
    group = "io.axiom"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java-library")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(listOf(
            "--enable-preview",
            "-Xlint:all"
        ))
    }
}
```

---

## Publishing Strategy

1. Each module publishes independently to Maven Central
2. BOM provides version alignment
3. Snapshots to OSSRH snapshots repository
4. Releases via staged release process
5. GPG signed artifacts
6. Source and Javadoc JARs required

---

*This document defines the module structure for Axiom.*
