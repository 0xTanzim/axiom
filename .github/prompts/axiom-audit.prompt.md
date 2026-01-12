---
description: 'Audit Axiom framework codebase. Fix critical defects, security risks, and architectural drift. Harden APIs and generate framework-grade documentation.'
name: 'Axiom Comprehensive Framework Audit'
agent: Axiom_Framework_Engineer
argument-hint: 'Optional: target runtime adapter (jdk, netty) or module name'
---

# Axiom Framework Audit, Hardening, and Documentation

## Mission

Audit the **Axiom framework codebase**.
Identify and fix **critical bugs**, **security risks**, and **architectural violations**.
Harden public APIs, improve framework structure, and generate **world-class framework documentation**.

This is **framework-level work**, not application refactoring.

---

## Scope & Preconditions

- Target: **current repository**
- Language: **Java 25 (LTS)**
- Architecture: **core + runtime adapters**
- Design source of truth: `/draft` RFCs

### Audit Focus Areas

- functional defects
- API misuse risks
- security vulnerabilities
- architectural drift
- SRP violations
- runtime adapter isolation
- lifecycle correctness
- edge-case safety
- documentation gaps

### Mandatory Rules

- **Critical issues are must-fix**
- Avoid breaking public APIs unless unavoidable
- If breaking changes are required:
  - document clearly
  - justify via RFC alignment

---

## Inputs

- Workspace: `${workspaceFolder}` (auto-detect)
- Language & build system: Maven / Gradle (auto-detect)
- Optional user inputs:
  - `${input:targetRuntime:runtime adapter (jdk, netty, etc)?}`
  - `${input:moduleName:core or specific module?}`

If essential information cannot be derived autonomously:
- classify as **Critical Gap**
- document and escalate
- do NOT guess

---

## Workflow

### 1) Framework Understanding

1. Read repository structure
2. Identify:
   - core modules
   - runtime adapters
   - public API boundaries
3. Map:
   - entry points
   - lifecycle flow
   - routing / request flow (if present)
4. Cross-check with relevant RFCs
5. Identify **contract vs implementation** boundaries

---

### 2) Bug & Vulnerability Audit

Search for:

- unhandled exceptions
- silent failures
- invalid state transitions
- unsafe concurrency
- resource leaks
- insecure file or network usage
- unsafe deserialization
- weak randomness or crypto
- dependency CVEs

Classify findings:
- **Critical** (must fix)
- **High**
- **Medium**
- **Low**

---

### 3) Framework Hardening & Fixes

Execute in priority order:

1. Fix **critical and high** issues first
2. Harden:
   - lifecycle transitions
   - public API validation
   - error propagation paths
3. Enforce:
   - explicit failure modes
   - deterministic behavior
4. Remove:
   - dead code
   - unused abstractions
   - speculative flexibility
5. Replace unsafe APIs with safer equivalents

All fixes must preserve framework intent.

---

### 4) Architecture & Design Audit

Bring code to **framework-grade quality**:

- Strict SRP enforcement
- No god objects
- Small, focused abstractions
- No core → runtime dependency leaks
- Explicit adapter boundaries
- No cyclic dependencies
- Clear ownership of responsibilities

Flag and fix architectural drift.

---

### 5) API Surface Review (Critical)

For all **public APIs**:

- Detect:
  - unclear contracts
  - misuse-prone methods
  - leaky abstractions
- Improve:
  - naming
  - immutability
  - error signaling
- Minimize surface area where possible
- Document intended usage clearly

Breaking changes require justification.

---

### 6) Runtime Compatibility Audit

1. Verify Java **25** compatibility
2. Review:
   - threading model
   - virtual thread usage
   - blocking vs non-blocking paths
3. Validate runtime adapters:
   - no shared mutable state
   - clean lifecycle hooks
4. Remove runtime-specific logic from core

---

### 7) Edge Case & Failure Mode Coverage

Ensure handling for:

- null / invalid input
- boundary values
- lifecycle misuse
- I/O failures
- concurrency hazards
- shutdown and restart behavior
- partial initialization
- timeout and retry logic (where appropriate)

Frameworks must fail **predictably and loudly**.

---

### 8) Comparative Framework Review

Compare Axiom with similar frameworks:

- Identify best practices worth adopting
- Identify anti-patterns to avoid
- Improve Axiom without copying design flaws
- Preserve Axiom’s explicit, non-magical philosophy

Document lessons learned briefly.

---

### 9) Documentation Tasks

#### README.md (Framework-Level)

Ensure README includes:

- clear vision and scope
- core concepts
- install & minimal usage
- API philosophy
- runtime adapters overview
- compatibility matrix (Java 25)
- security considerations
- contribution guidelines

#### Framework Docs (VitePress or Markdown)

Docs must be:

- concise
- accurate
- runnable examples
- explicit about contracts
- written for framework users (developers)

Include:
- lifecycle overview
- routing (if applicable)
- runtime adapter model
- extension points

---

### 10) Quality Gate (Mandatory)

Before completion, verify:

- build passes
- tests pass
- no architectural violations remain
- public APIs are documented
- docs build without errors

---

## Output Expectations

- Fixed and improved source files
- Summary of key changes
- Risk notes for unresolved low-priority issues
- Updated or generated README.md
- Documentation skeleton and core pages

---

## Quality Assurance Checklist

- [ ] Critical vulnerabilities fixed
- [ ] Architectural drift resolved
- [ ] Core/runtime separation enforced
- [ ] Public APIs hardened
- [ ] Edge cases handled
- [ ] Java 25 compliance verified
- [ ] Docs are complete and clear

---

## Failure Triggers

Escalate only if:

- repository cannot be analyzed
- build system is missing or broken
- RFC intent cannot be determined
- runtime behavior is technically impossible to validate

---

## Recommended Next Steps (Post-Audit)

- add CI static analysis
- add API compatibility checks
- add lifecycle stress tests
- add fuzz testing for public APIs
- define versioning and deprecation policy
