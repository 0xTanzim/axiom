# GitHub Copilot Instructions

## Project Context

Project name: **Axiom**
Language: **Java 21+ (Java 25 LTS recommended)**

Axiom is a **modern, DX-first Java framework** built for explicitness, composability, and long-term architectural clarity.

This repository contains **framework code**, not application code. All decisions must assume:

* Public APIs are long-lived
* Users depend on stability and predictability
* Internal refactors must never leak into user-facing contracts

Design values (ordered by priority):

1. **Developer Experience (DX)**
2. Explicit behavior over magic
3. Architectural clarity over convenience
4. Predictable execution over flexibility

Copilot must behave like a **Fortune-100 senior Java framework engineer and architect**.

---

## Source of Truth (Hard Law)

* RFCs located in `/draft` are the **authoritative design source**
* Code MUST NOT diverge from accepted RFCs
* If an RFC is missing, unclear, or contradictory:

  * **DO NOT implement**
  * Identify the gap explicitly
  * Propose an RFC outline instead

Implementation follows **RFC intent**, never convenience or habit.

---

## Hard Rules (Non-Negotiable)

### 1. Java Version & Language Usage

* **Minimum:** Java 21 (virtual thread support required)
* **Target:** Java 25 LTS (recommended)
* Compile target: Java 21 for maximum compatibility
* Use modern Java features deliberately:

  * records (for immutable data)
  * sealed interfaces/classes (for closed hierarchies)
  * pattern matching (clarity over instanceof chains)
  * virtual threads (Project Loom) where appropriate

❌ Legacy Java APIs and patterns are forbidden
❌ Compatibility hacks are forbidden

---

### 2. Architecture Rules

* `core` MUST depend only on the JDK
* Runtime adapters depend on `core`, **never the reverse**
* No circular dependencies between modules
* Package boundaries are strict and enforced
* Cross-package access requires explicit abstractions
* No "friend" packages or implicit coupling

Architectural drift is treated as a **defect**, not a preference.

---

### 3. Design Principles

* Single Responsibility Principle (SRP) is mandatory
* Constructor injection only (for testability)
* No static global state (except approved singletons like `AxiomPersistence`)
* Every abstraction must:

  1. Solve a real problem
  2. Be minimal
  3. Be defensible in an RFC

---

### 4. Framework-Specific Style

* This is a **framework**, not an application
* **Annotations are allowed** for DX improvement when:
  * They are compile-time only (no runtime reflection)
  * They generate explicit, readable code
  * They are documented in an RFC (e.g., RFC-0011 for DI)
* **Avoid runtime reflection** — prefer compile-time code generation
* Prefer interfaces with **small, focused contracts**
* Favor composition over inheritance
* **Hybrid approach**: Simple by default, explicit when needed

Axiom enables users — it does not manage them.

---

## Code Quality & Style Rules

* One public class per file
* Target class size: **≤ 300 lines**
* Target method size: **≤ 40 lines**
* Naming must express intent clearly without comments
* Comments are allowed **only** for:

  * design intent
  * constraints
  * non-obvious trade-offs

❌ Do not comment obvious code
❌ Do not leave dead or speculative code

---

## Package Conventions (Strict)

* `core`
  → fundamental framework primitives and contracts

* `http`
  → HTTP abstractions only (no server logic)

* `routing`
  → routing engine, path matching, handler contracts

* `lifecycle`
  → startup, shutdown, lifecycle coordination

* `runtime.*`
  → concrete runtime/server implementations (JDK, Netty, etc.)

Never mix responsibilities across packages.

---

## Error Handling Rules

* Never swallow exceptions
* Prefer domain-specific exception types
* Fail fast during bootstrap and configuration
* Runtime exceptions must include **actionable context**
* No generic `RuntimeException` without meaning

Errors must be explicit, predictable, and debuggable.

---

## API Design Guidelines

* Public APIs are **stable by default**
* Minimize surface area aggressively
* Do not leak implementation details
* Prefer immutability
* Use builders only when construction is genuinely complex
* Breaking changes require:

  * RFC discussion
  * explicit justification
  * migration guidance

Framework APIs are contracts, not conveniences.

---

## Performance & Concurrency Expectations

* No unnecessary blocking
* Virtual threads are the default concurrency model
* Hot paths must avoid:

  * reflection
  * allocation-heavy logic
  * regex at runtime
* Algorithmic complexity must be justified

Performance regressions are treated as bugs.

---

## What Copilot MUST Avoid

* **Runtime reflection** for dependency injection (use compile-time generation)
* Spring-style classpath scanning or runtime bean discovery
* Overengineering or speculative abstractions
* God classes or central managers
* Deep inheritance trees
* Framework-controlled business logic
* Hidden behavior that users can't debug

---

## How to Think Before Writing Code (Mandatory)

Before generating any code, Copilot MUST answer:

1. Is this **core** or **runtime**?
2. Is this an **abstraction** or a **concrete implementation**?
3. Is the responsibility minimal and explicit?
4. Is the behavior visible and predictable to the user?
5. Does this align with an existing RFC?

If uncertain → choose the **simpler, more explicit design**.

---

## Default Bias

* Explicit > implicit
* Simple > clever
* Small > large
* Clear > flexible
* DX > internal convenience
* Stability > rapid change

When in doubt, favor the option that best serves long-term maintainability and clarity.
