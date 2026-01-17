---
description: 'Global rules that apply to all work in the Axiom repository. Defines universal behavior, quality bar, and decision hierarchy for code, docs, prompts, and agents.'
applyTo: '**'
---

# Axiom Global Instructions

## Purpose

This file defines **repository-wide laws** that apply to **all work** in Axiom:

* code
* documentation
* prompts
* agents
* refactors
* audits

If any other instruction, prompt, or agent conflicts with this file, **this file wins**.

---

## Decision Hierarchy (Strict)

All decisions MUST follow this order:

1. **RFCs in `/draft`** (highest authority)
2. **`global.instructions.md`** (this file)
3. **`copilot-instructions.md`** (code-specific law)
4. **Domain instruction files** (e.g. `docs.instructions.md`)
5. **Prompts** (`*.prompt.md`)
6. **Agents** (`*.agent.md`)

Lower layers may refine behavior but MUST NOT violate higher layers.

---

## Non-Negotiable Principles

* **DX-first**: user experience outweighs internal convenience
* **Explicit over implicit**: no hidden behavior
* **Simplicity over cleverness**
* **Long-term maintainability over short-term speed**
* **Framework purity**: Axiom is a framework, not an application

---

## Scope Boundaries

### What Axiom IS

* A Java framework (Java 25 LTS)
* Explicit, composable, predictable
* Runtime-agnostic via adapters

### What Axiom IS NOT

* An application framework with magic
* Annotation-driven
* Reflection-heavy
* Opinionated about user business logic

---

## Change Discipline

* No speculative abstractions
* No unused code or docs
* No premature optimization
* No breaking public APIs without RFC approval

Every change must have a **clear justification**.

---

## Quality Bar (Universal)

All outputs MUST be:

* correct
* intentional
* minimal
* documented
* aligned with RFC intent

If quality is uncertain → **stop and escalate**.

---

## Failure Handling

* Fail fast
* Fail loudly
* Fail with context

Silent failure is forbidden.

---

## Documentation Rule (Universal)

If behavior exists:

* it must be documented

If documentation exists:

* it must reflect real behavior

Docs and code are equal citizens.

---

## Tooling & Automation

* Automation exists to enforce discipline, not bypass it
* Agents may act autonomously but are bound by this file
* Prompts guide execution but cannot override law

---

## Core Rule

> If a decision makes Axiom harder to understand, use, or maintain,
> it is the wrong decision.

---

This file defines the **global constitution** of the Axiom repository.
