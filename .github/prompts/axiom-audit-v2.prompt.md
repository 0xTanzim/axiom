# Axiom Framework Audit v2 — Enhanced

## Mission

Audit the **Axiom framework codebase** with RFC alignment verification.
Identify and fix **critical bugs**, **security risks**, and **architectural violations**.
Harden public APIs, improve framework structure, and generate **world-class documentation**.

This is **framework-level work**, not application refactoring.

---

## Scope & Preconditions

- Target: **current repository**
- Language: **Java 21+ (minimum), Java 25 LTS (target)**
- Architecture: **core + runtime adapters** (strict separation)
- Design source of truth: `/draft` RFCs

### Audit Focus Areas

- RFC alignment verification (CRITICAL)
- Functional defects
- API misuse risks
- Security vulnerabilities
- Architectural drift from RFC intent
- SRP violations
- Core/runtime boundary violations
- Lifecycle correctness
- Hot path performance
- Edge-case safety
- Documentation gaps

---

## Phase 0: RFC Alignment Check (MANDATORY)

**Before any code audit, verify implementation matches RFC intent.**

1. Map each module to corresponding RFC(s)
2. Verify contracts match RFC definitions exactly
3. Flag any implementation drift as **architectural defect**
4. Identify missing RFCs as **Critical Gaps**

### RFC Mapping

| Module | Primary RFC | Supporting RFCs |
|--------|-------------|-----------------|
| axiom-core/handler | RFC-0001 | - |
| axiom-core/context | RFC-0001 | RFC-0005 |
| axiom-core/routing | RFC-0002, RFC-0003 | - |
| axiom-core/middleware | RFC-0004 | RFC-0005 |
| axiom-http | RFC-0001 | - |
| axiom-runtime-* | RFC-0006 | RFC-0007 (pending) |

### Critical RFC Gaps (Block Implementation)

- RFC-0007: Lifecycle Management — **NEEDED**
- RFC-0008: Error Handling Architecture — **NEEDED**
- RFC-0009: Runtime Adapter Contract — **NEEDED**
- RFC-0010: Testing Utilities API — **NEEDED**

---

## Phase 1: Java 25 Feature Audit

### Modern Java Compliance

- [ ] Records used for immutable data types
- [ ] Sealed interfaces for controlled extension
- [ ] Pattern matching in switch statements
- [ ] `Optional` used instead of null returns
- [ ] `ScopedValue` instead of `ThreadLocal`
- [ ] Virtual threads enabled by default
- [ ] No `synchronized` blocks with blocking I/O (pinning)
- [ ] Text blocks for multi-line strings
- [ ] `var` used appropriately (local inference)

### Virtual Thread Checklist

- [ ] Request handling on virtual threads
- [ ] No carrier thread pinning
- [ ] `ReentrantLock` over `synchronized`
- [ ] Document blocking I/O patterns
- [ ] Test on Java 21, 23, 25

---

## Phase 2: Hot Path Performance Audit

### Critical Hot Paths

1. Request → Context creation
2. Path parsing → Route match
3. Middleware chain execution
4. Handler invocation
5. Response writing

### Performance Requirements

| Operation | Target | Measurement |
|-----------|--------|-------------|
| Route match | < 100ns | JMH benchmark |
| Middleware (3-layer) | < 500ns | JMH benchmark |
| Full request (hello world) | < 10μs | End-to-end |
| Throughput | 100K+ req/s | Wrk/hey benchmark |

### Allocation Audit

- [ ] Zero allocations in route matching (after startup)
- [ ] Pre-sized param maps based on route definition
- [ ] Interned strings for HTTP method/status
- [ ] Lazy body parsing (only on access)
- [ ] Direct buffer writes (no intermediate copies)

### Forbidden in Hot Path

- ❌ Reflection
- ❌ Regex matching per request
- ❌ New lambda creation per request
- ❌ Synchronized blocks with I/O
- ❌ Thread-local lookups (prefer ScopedValue)

---

## Phase 3: API Surface Review

### Public API Principles

- [ ] Minimal surface area (fewer exports = fewer breaks)
- [ ] No implementation leakage
- [ ] Immutable types preferred
- [ ] Builder pattern for complex construction
- [ ] Functional interfaces for extensibility
- [ ] Clear error signaling (typed exceptions)

### API Stability Tiers

| Package | Stability | Rule |
|---------|-----------|------|
| `io.axiom.core.*` | Public | Stable after 1.0 |
| `io.axiom.http.*` | Public | Stable after 1.0 |
| `io.axiom.*.internal` | Internal | May change without notice |
| `io.axiom.runtime.*` | SPI | Stable contract, impl varies |

### Breaking Change Detection

- [ ] Public method signature changes
- [ ] Return type changes
- [ ] Exception type changes
- [ ] Semantic behavior changes
- [ ] Default value changes

---

## Phase 4: Runtime Adapter Audit

### Adapter Contract Verification

- [ ] Implements `Server` interface exactly
- [ ] No core → adapter dependency
- [ ] Clean lifecycle integration
- [ ] Proper error propagation
- [ ] Virtual thread support verified
- [ ] No shared mutable state between requests

### Adapter-Specific Checks

**JDK HttpServer:**
- [ ] Uses `HttpServer.create()` correctly
- [ ] Virtual thread executor configured
- [ ] Graceful shutdown implemented
- [ ] SSL/TLS optional support

**Netty (future):**
- [ ] EventLoop configured correctly
- [ ] ByteBuf lifecycle managed
- [ ] Pipeline setup efficient
- [ ] Native transport optional

---

## Phase 5: Security Audit

### Input Validation

- [ ] Path traversal prevention
- [ ] Header injection prevention (CRLF)
- [ ] Body size limits enforced
- [ ] Content-Type validation

### Output Safety

- [ ] JSON encoding for user data
- [ ] Header value sanitization
- [ ] Error messages don't leak internals

### Dependency Security

- [ ] No known CVEs in dependencies
- [ ] Minimal dependency footprint
- [ ] SBOM generation capability

---

## Phase 6: Edge Case Coverage

### Must Handle

- [ ] Null/invalid input parameters
- [ ] Boundary values (empty strings, max integers)
- [ ] Lifecycle misuse (double start, stop when not running)
- [ ] I/O failures (connection drops)
- [ ] Concurrent access patterns
- [ ] Shutdown during active requests
- [ ] Partial initialization failure
- [ ] Large request/response bodies
- [ ] Malformed HTTP (delegated to adapter)

Reference: `/docs/architecture/EDGE_CASES.md`

---

## Phase 7: Documentation Audit

### Required Documentation

- [ ] README.md (vision, install, quick start)
- [ ] ARCHITECTURE.md (technical design)
- [ ] MODULES.md (project structure)
- [ ] DECISIONS.md (ADRs)
- [ ] ROADMAP.md (implementation plan)
- [ ] PUBLISHING.md (release process)
- [ ] API Javadoc (100% public coverage)
- [ ] User Guide (getting started, routing, middleware)

### Documentation Quality

- [ ] Accurate (matches implementation)
- [ ] Concise (no fluff)
- [ ] Runnable examples
- [ ] Explicit about contracts
- [ ] Written for framework users

---

## Phase 8: Quality Gate (Mandatory)

Before completion, verify ALL:

- [ ] Build passes (all modules)
- [ ] Tests pass (all Java versions: 21, 23, 25)
- [ ] Test coverage > 90%
- [ ] No architectural violations
- [ ] No RFC drift
- [ ] Public APIs documented
- [ ] Docs build without errors
- [ ] Benchmarks pass targets

---

## Output Expectations

1. **Audit Report** — Issues found, severity, resolution
2. **Fixed Source Files** — Direct fixes applied
3. **RFC Gap Summary** — Missing RFCs identified
4. **Updated Documentation** — All docs current
5. **Risk Register** — Unresolved low-priority items

---

## Severity Classification

| Severity | Definition | Action |
|----------|------------|--------|
| Critical | Blocks functionality, security flaw | Must fix immediately |
| High | Significant bug, API misuse risk | Fix before release |
| Medium | Suboptimal but functional | Fix in current phase |
| Low | Minor improvement | Track for future |

---

## Escalation Triggers

Escalate ONLY if:

- Repository cannot be analyzed
- Build system broken
- RFC intent cannot be determined
- Technical impossibility proven
- External dependency blocks progress

### Escalation Format

```
ESCALATION
Type: [RFC Gap | Technical | External]
Context: [What you're trying to do]
Attempts Made: [What you tried]
Root Blocker: [Specific issue]
Impact: [What's blocked]
Required Action: [What's needed to unblock]
```

---

## Comparison Checklist

Compare with peer frameworks:

- [ ] Hono (TypeScript) — DX patterns
- [ ] Javalin (Java) — API simplicity
- [ ] Vert.x (Java) — Performance patterns
- [ ] Spring WebFlux — What NOT to do
- [ ] Express (Node) — Middleware model

Adopt best practices. Avoid anti-patterns.

---

## Post-Audit Recommendations

After audit complete:

1. Add CI static analysis (SpotBugs, Error Prone)
2. Add API compatibility checks (japicmp)
3. Add lifecycle stress tests
4. Add fuzz testing for public APIs
5. Define versioning and deprecation policy
6. Set up security scanning (Dependabot, Snyk)

---

*This audit prompt ensures comprehensive, RFC-aligned framework quality.*
