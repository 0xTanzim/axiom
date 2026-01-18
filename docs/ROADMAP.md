# Axiom Framework — Roadmap

**Last Updated:** January 18, 2026
**Current Phase:** Phase 1 Complete (97%)
**Tests:** 305 passing
**Next Priority:** RFC-0010 Testing Utilities

---

## Vision

Axiom is a **DX-first, explicit Java web framework** for Java 25+.

Unlike Spring Boot or Quarkus, Axiom:
- Has **zero magic** — no classpath scanning, no reflection-based injection
- Is **explicit by design** — configuration is code, not hidden annotations
- Is **minimal by default** — add only what you need
- Embraces **virtual threads** natively (Project Loom)

Our goal: The simplest, most predictable Java web framework that scales.

---

## Phase 1: Foundation (95% Complete) ✅

| Component | Status | Tests |
|-----------|--------|-------|
| Core (Handler, Context, Router) | ✅ | 135 |
| Trie-based routing with params | ✅ | included |
| Middleware pipeline | ✅ | included |
| App composition | ✅ | included |
| Lifecycle hooks | ✅ | included |
| Error handling | ✅ | included |
| JDK HttpServer adapter | ✅ | 9 |
| Virtual thread support | ✅ | included |
| SLF4J logging + MDC | ✅ | included |
| Persistence (JDBC/JPA/jOOQ) | ✅ | 44 |
| @Transactional code gen | ✅ | 16 |
| Configuration (.env, profiles) | ✅ | 43 |
| Validation (Jakarta) | ✅ | 48 |
| **Testing Utilities** | ❌ | 0 |

**Remaining for Phase 1:** RFC-0010 Testing Utilities only.

---

## Phase 1 Completion: RFC-0010 Testing Utilities

### Goal
Enable users to test Axiom applications without starting a real server.

### Planned Features

```java
// In-memory testing without HTTP
AxiomTest test = AxiomTest.wrap(app);

TestResponse response = test.get("/users/123")
    .header("Authorization", "Bearer token")
    .execute();

assertThat(response.status()).isEqualTo(200);
assertThat(response.bodyAs(User.class).name()).isEqualTo("Alice");
```

### Implementation Plan

1. `AxiomTest` — wraps App for in-memory execution
2. `TestRequest` — builder for test requests
3. `TestResponse` — response with status, headers, body access
4. `MockContext` — injectable test context
5. Integration with JUnit 5 extension (optional)

### Estimated Effort
- **Tests:** ~40-50 new tests
- **Time:** 1-2 days
- **Priority:** HIGH

---

## Phase 2: Production Readiness (Planned)

| Feature | RFC | Priority |
|---------|-----|----------|
| Metrics (Micrometer) | TBD | High |
| Health checks | TBD | High |
| Graceful shutdown | Partial | Medium |
| Request timeout | TBD | Medium |
| Rate limiting | TBD | Medium |
| Compression (gzip) | TBD | Low |

### Rationale
Before production deployment, users need observability and operational controls.

---

## Phase 3: Ecosystem (Future)

| Feature | RFC | Priority |
|---------|-----|----------|
| WebSocket support | TBD | Medium |
| Server-Sent Events | TBD | Medium |
| OpenAPI generation | TBD | Low |
| Alternative runtime (Netty) | TBD | Low |

---

## Known DX Issues

### ✅ FIXED: Error Messages
**Status:** All exceptions include context (RouteNotFoundException, MethodNotAllowedException, ConfigException.Missing, etc.)

### ✅ FIXED: @Transactional without Processor
**Status:** Added `TransactionalValidator` class that detects missing processor and provides detailed fix instructions.

### ✅ FIXED: Configuration Errors Not Clear
**Status:** `ConfigException` has subtypes: `Missing`, `WrongType`, `BindingFailed` with full context.

### 4. No IDE Support for Route Discovery
**Issue:** Users can't Ctrl+Click from route string to handler.
**Fix:** Future IDE plugin or OpenAPI integration.

---

## Technical Debt

| Item | Impact | Priority |
|------|--------|----------|
| Centralize exception messages | Medium | Medium |
| Reduce allocation in hot path | Low | Low |
| Add benchmark suite | Medium | Medium |
| Document all public APIs (Javadoc) | High | High |

---

## Module Architecture Explanation

### Why `axiom-persistence` and `axiom-persistence-processor`?

**Short answer:** Java annotation processors MUST be in separate modules.

**Detailed explanation:**

```
axiom-persistence          (RUNTIME module)
├── @Transactional         annotation definition
├── Transaction            runtime transaction management
├── DataSourceFactory      connection pool management
├── Jdbc, Jpa, Jooq       database access utilities
└── Used at: RUNTIME

axiom-persistence-processor (COMPILE-TIME module)
├── TransactionalProcessor annotation processor
├── Uses JavaPoet          to generate Java source code
├── Generates: *$Tx.java   wrapper classes
└── Used at: COMPILE TIME ONLY
```

**Why separate?**

1. **Java spec requirement** — Annotation processors run during compilation, before runtime classes exist
2. **Classpath isolation** — Processor dependencies (JavaPoet) shouldn't be in user's runtime
3. **Build tool integration** — Maven/Gradle need processors in `annotationProcessorPaths`
4. **Zero runtime overhead** — Generated code is plain Java, no reflection

**Example flow:**

```java
// User writes:
public class UserService {
    @Transactional
    public void save(User u) { ... }
}

// At compile time, processor generates:
public class UserService$Tx extends UserService {
    private final DataSource ds;

    @Override
    public void save(User u) {
        Transaction.execute(ds, () -> super.save(u));
    }
}

// User uses generated class (explicit, debuggable):
UserService$Tx service = new UserService$Tx(dataSource);
service.save(user);  // Transaction handled by generated code
```

**DX benefit:** Users see the generated code. No magic. Easy debugging.

---

## Success Criteria for 1.0

- [ ] All 14 RFCs implemented
- [ ] >300 tests passing
- [ ] Full Javadoc coverage
- [ ] Getting Started guide
- [ ] Benchmark suite
- [ ] No critical DX issues

---

## Development Experience

### Hot Reload Options

Axiom supports several hot reload solutions for development:

#### Option 1: JBang (Recommended for Quick Scripts)
```bash
# Install jbang
curl -Ls https://sh.jbang.dev | bash

# Run with watch mode
jbang --watch Application.java
```

#### Option 2: Spring Boot DevTools (Maven/Gradle)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <version>3.2.0</version>
    <scope>runtime</scope>
</dependency>
```
Note: DevTools watches for class changes, not Axiom-specific.

#### Option 3: JRebel (Commercial)
```bash
# Add JRebel agent
java -agentpath:/path/to/jrebel/lib/libjrebel64.so -jar app.jar
```

#### Option 4: DCEVM + HotswapAgent (Free)
```bash
# Use DCEVM JDK build
# Add HotswapAgent
java -XX:+AllowEnhancedClassRedefinition -javaagent:hotswap-agent.jar -jar app.jar
```

#### Option 5: IDE Built-in (IntelliJ IDEA)
```
Settings → Build → Compiler → Build project automatically
Registry → compiler.automake.allow.when.app.running = true
```

### Recommended Dev Workflow

```bash
# Terminal 1: Watch and compile
mvn compile -Dmaven.compiler.useIncrementalCompilation=true -q -f

# Terminal 2: Run with class reload
java --enable-preview -XX:+EnableDynamicAgentLoading -jar target/app.jar
```

---

## Maven Ecosystem Compatibility

### ✅ Fully Compatible

Axiom works with the entire Maven ecosystem:

| Tool | Status | Notes |
|------|--------|-------|
| Maven Central | ✅ | Standard deployment |
| Gradle | ✅ | Works identically |
| Maven Shade Plugin | ✅ | Fat JAR creation |
| Maven Assembly | ✅ | Custom packaging |
| Maven Wrapper | ✅ | Portable builds |
| Nexus/Artifactory | ✅ | Private repos |
| Dependabot | ✅ | Auto-updates |

### Module System (JPMS)

Axiom uses Java modules (`module-info.java`):

```java
module my.app {
    requires io.axiom.core;
    requires io.axiom.server;
    requires io.axiom.config;      // optional
    requires io.axiom.validation;  // optional
    requires io.axiom.persistence; // optional
}
```

For classpath mode (no modules), just use as regular dependencies.

---

## Lombok Compatibility

### ✅ Fully Compatible

Lombok works perfectly with Axiom:

```xml
<dependencies>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.34</version>
        <scope>provided</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <!-- Lombok FIRST (generates getters/setters) -->
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>1.18.34</version>
                    </path>
                    <!-- Axiom processor SECOND -->
                    <path>
                        <groupId>io.axiom</groupId>
                        <artifactId>axiom-persistence-processor</artifactId>
                        <version>${axiom.version}</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Lombok Usage with Axiom

```java
@Data
@AllArgsConstructor
public class User {
    private Long id;
    private String email;
    private String name;
}

// Works with Axiom validation
@Data
public class CreateUserRequest {
    @NotBlank
    private String email;

    @Size(min = 8)
    private String password;
}

// Works with @Transactional
@RequiredArgsConstructor
public class UserService {
    private final DataSource dataSource;

    @Transactional
    public void save(User user) {
        // Lombok generates constructor
    }
}
```

### Recommendation

While Lombok works, we recommend **Java records** for DTOs:

```java
// Preferred: Java record (built-in, no annotation processor)
public record User(Long id, String email, String name) {}

// Also works: Lombok
@Data
public class User {
    private Long id;
    private String email;
    private String name;
}
```

Records are:
- Built into Java 16+
- No annotation processor needed
- Immutable by default
- Less magic

---

## Example Applications

### Available Examples

| Example | Location | Features |
|---------|----------|----------|
| Hello World | `examples/hello-world/` | Basic setup |
| Auth Example | `examples/axiom-auth-example/` | Login, JWT, DB, Validation |

### Auth Example Flow

```
examples/axiom-auth-example/
├── Application.java      ← Entry point, wires everything
├── config/
│   └── AppConfig.java    ← Type-safe config (@ConfigMapping)
├── domain/
│   └── User.java         ← Entity (Java record)
├── dto/
│   ├── RegisterRequest   ← Validated input
│   ├── LoginRequest      ← Validated input
│   └── AuthResponse      ← JWT response
├── repository/
│   └── UserRepository    ← DB access (@Transactional)
├── service/
│   ├── AuthService       ← Business logic
│   └── JwtService        ← Token handling
├── middleware/
│   └── AuthMiddleware    ← JWT validation
└── routes/
    ├── AuthRoutes        ← POST /auth/register, /auth/login
    └── UserRoutes        ← GET/PUT /users/me (protected)
```

---

## Contributing

1. Pick an item from this roadmap
2. Check the corresponding RFC in `/draft`
3. Implement with tests
4. Submit PR

All contributions must follow:
- `copilot-instructions.md` (code law)
- `docs.instructions.md` (doc law)
- `global.instructions.md` (universal law)
