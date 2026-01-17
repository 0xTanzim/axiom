---

description: 'Comprehensive, RFC-driven audit and hardening prompt for the Axiom framework. Enforces Java 25 best practices, DX-first APIs, performance discipline, and framework-grade documentation.'
name: 'Axiom Unified Framework Audit'
agent: Axiom_Framework_Engineer
argument-hint: 'Optional: target runtime adapter (jdk, netty) or module name'
-----------------------------------------------------------------------------

# Axiom Framework Audit — Unified & Hardened

## Mission

Audit the **Axiom framework codebase** with **strict RFC alignment**.
Identify and fix **critical defects**, **security risks**, **architectural drift**, and **performance regressions**.
Harden public APIs, enforce clean architecture, and generate **framework-grade documentation**.

This is **framework-level work**, not application refactoring.

---

## Scope & Preconditions

* Target: **current repository**
* Language: **Java 25 (LTS)** (Java 21/23 allowed for validation only)
* Architecture: **core + runtime adapters** (strict separation)
* Source of truth: `/draft` RFCs

---

## Phase 0: RFC Alignment Check (MANDATORY)

**Before any code changes, verify implementation matches RFC intent.**

1. Map each module to its governing RFC(s)
2. Verify contracts match RFC definitions exactly
3. Flag implementation drift as **architectural defects**
4. Identify missing RFCs as **Critical Gaps**

### Known RFC Gaps (Block Implementation)

* RFC-0007: Lifecycle Management
* RFC-0008: Error Handling Architecture
* RFC-0009: Runtime Adapter Contract
* RFC-0010: Testing Utilities API
* RFC-0011: Persistence & Transaction
* RFC-0012: Logging (SLF4J)
* RFC-0013: Configuration System



If a gap blocks correctness:

* Draft an RFC outline
* Continue **only** at design and documentation level

Never guess framework behavior.

---

## Phase 1: Java 25 & Concurrency Audit

### Modern Java Compliance

* Records for immutable data
* Sealed interfaces/classes for closed hierarchies
* Pattern matching in `switch`
* `Optional` instead of null returns
* `ScopedValue` instead of `ThreadLocal`
* Virtual threads enabled by default
* No `synchronized` blocks with blocking I/O
* Text blocks for multi-line strings
* `var` used judiciously

### Virtual Thread Safety

* No carrier thread pinning
* Prefer `ReentrantLock` over `synchronized`
* Document blocking I/O behavior
* Validate on Java 21, 23, and 25

---

## Phase 2: Hot Path Performance Audit

### Critical Paths

1. Request → Context creation
2. Route parsing → match
3. Middleware chain execution
4. Handler invocation
5. Response writing

### Performance Targets

| Operation            | Target      |
| -------------------- | ----------- |
| Route match          | < 100ns     |
| Middleware (3-layer) | < 500ns     |
| Full request         | < 10μs      |
| Throughput           | 100K+ req/s |

### Allocation Rules

* Zero allocations in routing after startup
* Pre-sized param structures
* Lazy body parsing
* No per-request lambdas

❌ Forbidden in hot paths: reflection, regex, thread-local lookups

---

## Phase 3: Architecture & API Surface Review

### Architecture Enforcement

* Strict SRP
* No god objects
* No core → runtime dependency leaks
* Explicit adapter boundaries
* No cyclic dependencies

### API Principles

* Minimal, stable surface area
* No implementation leakage
* Immutability preferred
* Typed, explicit error signaling

### Stability Tiers

| Package               | Stability       |
| --------------------- | --------------- |
| `io.axiom.core.*`     | Public (stable) |
| `io.axiom.http.*`     | Public (stable) |
| `io.axiom.*.internal` | Internal        |
| `io.axiom.runtime.*`  | SPI             |

---

## Phase 4: Runtime Adapter Audit

* Contract matches `Server` interface exactly
* No shared mutable state
* Correct lifecycle integration
* Virtual thread support verified
* Clean error propagation

---

## Phase 5: Security Audit

* Input validation (path, headers, body size)
* Output safety (encoding, sanitization)
* No sensitive data leaks
* Dependency CVE scan
* Minimal dependency footprint

---

## Phase 6: Edge Case & Failure Modes

Must handle predictably:

* Invalid input
* Boundary values
* Lifecycle misuse
* Concurrent access
* Shutdown during active requests
* Partial initialization failure

Framework must fail **loudly and deterministically**.

---

## Phase 7: Documentation Audit

### Required Outputs

* README.md (vision, install, quick start)
* ARCHITECTURE.md
* MODULES.md
* DECISIONS.md (ADRs)
* ROADMAP.md
* Public API Javadoc (100%)
* User Guide

Docs must be accurate, concise, runnable, and user-focused.

---

## Phase 8: Quality Gate (MANDATORY)

All must pass:

* Build passes
* Tests pass (21, 23, 25)
* Coverage > 90%
* No RFC drift
* No architectural violations
* Benchmarks meet targets
* Docs build cleanly

---

## Output Expectations

1. Audit report (issues, severity, fixes)
2. Fixed source files
3. RFC gap summary
4. Updated documentation
5. Risk register

---

## Escalation Protocol

Escalate ONLY if:

* RFC intent cannot be resolved
* Technical impossibility proven
* External dependency blocks progress

### Escalation Format

```
ESCALATION
Type:
Context:
Attempts Made:
Root Blocker:
Impact:
Required Action:
```

---

## Post-Audit Actions

* CI static analysis
* API compatibility checks
* Lifecycle stress tests
* Fuzz testing
* Versioning & deprecation policy

---

*This unified audit prompt enforces RFC discipline, Java 25 correctness, performance rigor, and DX-first framework quality.*
