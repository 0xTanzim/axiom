# GitHub Copilot Instructions

## Project Context

Project name: **Axiom**
Language: **Java 25 (LTS)**

Axiom is a modern Java framework built for explicitness, composability,
and long-term architectural clarity.

This repository contains **framework code**, not application code.
Design decisions favor:
- clarity over magic
- explicit APIs over reflection
- modular systems over monoliths
- predictable behavior over convenience shortcuts

Copilot must behave like a **senior Java framework engineer** designing
public, long-lived APIs.

---

## Source of Truth

- RFCs located in `/draft` are the **authoritative design source**
- Code MUST NOT diverge from accepted RFCs
- If an RFC is missing or unclear, **do not implement** — request an RFC
- Implementation always follows RFC intent, not convenience

---

## Hard Rules (Non-Negotiable)

### 1. Java Version
- Target **Java 25 (LTS)** exclusively
- Do NOT introduce backward compatibility constraints
- Prefer modern Java features:
  - records
  - sealed interfaces
  - pattern matching
  - Optional
  - virtual threads (where appropriate)

Legacy Java patterns are forbidden.

---

### 2. Architecture Rules
- `core` MUST NOT depend on any runtime implementation
- Runtime adapters depend on `core`, never the opposite
- No circular dependencies between modules
- Package boundaries are strict and enforced
- Cross-package access requires explicit abstraction

---

### 3. Design Principles
- Single Responsibility Principle (SRP) is mandatory
- Constructor injection only
- No static global state
- No hidden lifecycle or implicit behavior
- Every abstraction must justify its existence

---

### 4. Framework Style
- This is a **framework**, not an application
- Avoid annotations unless explicitly designed and justified
- Avoid reflection unless required by an RFC
- Prefer interfaces with small, focused contracts
- Favor composition over inheritance

---

## Code Style Rules

- One public class per file
- Class size target: ≤ 300 lines
- Method size target: ≤ 40 lines
- Naming must explain intent without comments
- Comments are allowed only for:
  - design intent
  - constraints
  - non-obvious tradeoffs

Do not comment obvious code.

---

## Package Conventions

- `core`
  → fundamental framework primitives and contracts

- `http`
  → HTTP request / response abstractions only

- `routing`
  → routing engine, path matching, handler contracts

- `lifecycle`
  → startup, shutdown, hooks, lifecycle coordination

- `runtime.*`
  → concrete runtime/server implementations (JDK, Netty, etc.)

Never mix responsibilities across packages.

---

## Error Handling Rules

- Never swallow exceptions
- Prefer domain-specific exception types
- Fail fast during bootstrap and configuration
- Runtime exceptions must include actionable context
- No generic `RuntimeException` without meaning

---

## API Design Guidelines

- Public APIs are **stable by default**
- Minimize surface area
- Do not leak implementation details
- Prefer immutability
- Use builders only when object construction is complex
- Breaking changes require RFC discussion

---

## What Copilot MUST Avoid

- Spring-style magic or auto-wiring behavior
- Annotation-driven hidden logic
- Overengineering and speculative abstractions
- God classes or central managers
- Deep inheritance trees
- Framework-controlled business logic

Axiom empowers users — it does not control them.

---

## How to Think Before Writing Code

Before generating any code, Copilot MUST answer:

1. Is this **core** or **runtime**?
2. Is this an **abstraction** or a **concrete implementation**?
3. Is the responsibility minimal and clear?
4. Is the behavior explicit to the framework user?
5. Does this align with an existing RFC?

If uncertain → choose the **simpler, more explicit design**.

---

## Default Bias

- Explicit > implicit
- Simple > clever
- Small > large
- Clear > flexible
