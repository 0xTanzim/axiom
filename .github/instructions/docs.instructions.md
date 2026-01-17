---
description: 'Documentation standards and rules for the Axiom framework. Enforces clarity, accuracy, DX-first writing, and strict alignment with code and RFCs.'
applyTo: '**/*.md, **/*.mdx'
---

# Axiom Documentation Instructions

## Purpose

This instruction file defines **how documentation is written and maintained** for the Axiom framework.

Documentation is treated as a **first-class artifact**, equal in importance to code.

Docs exist to:

* Protect Developer Experience (DX)
* Make framework behavior obvious and predictable
* Prevent misuse of public APIs
* Preserve architectural intent over time

---

## Source of Truth

* **Code + RFCs are authoritative**
* Documentation MUST match:

  * actual behavior
  * public API contracts
  * RFC intent

If documentation and code disagree:

* **Code is considered incorrect until verified**
* The mismatch MUST be resolved immediately

---

## Documentation Scope

### Required Documentation Types

* `README.md` — vision, scope, quick start
* `ARCHITECTURE.md` — system design and boundaries
* `MODULES.md` — module responsibilities and dependencies
* `DECISIONS.md` — architectural decision records (ADRs)
* `ROADMAP.md` — planned phases and future work
* User guides — routing, middleware, lifecycle, runtime adapters
* API documentation — all public types and methods

---

## Writing Principles (Non-Negotiable)

* **DX-first**: optimize for the reader, not the writer
* **Explicit over implicit**: never assume hidden knowledge
* **Concrete over abstract**: prefer examples over theory
* **Concise**: no filler, no marketing language
* **Honest**: document limitations and non-goals

Avoid:

* vague descriptions
* aspirational statements not backed by code
* future promises mixed into current behavior

---

## Style Rules

* Clear, direct language
* Short sentences
* Active voice
* Consistent terminology across all docs

Terminology rules:

* If a term appears in public API, it MUST be used consistently
* New terms MUST be defined once and reused

---

## Code Examples

All examples MUST:

* compile (where applicable)
* reflect current APIs exactly
* avoid deprecated features
* use Java **25** syntax only

Examples must show:

* the simplest correct usage
* no unnecessary configuration
* no framework internals

If an example requires explanation, the API is likely too complex.

---

## MDX-Specific Rules

When using MDX:

* Separate prose from code blocks clearly
* Avoid large, monolithic examples
* Prefer multiple small examples
* Do not embed complex logic inside MDX

MDX is for **learning**, not implementation detail.

---

## API Documentation Rules

For every **public** type:

* Purpose must be documented
* Lifecycle expectations must be explicit
* Thread-safety must be stated
* Error behavior must be described

For every **public** method:

* Inputs and constraints documented
* Side effects documented
* Exceptions documented (typed, not generic)

No undocumented public APIs are allowed.

---

## Versioning & Stability Markers

Documentation MUST indicate:

* Stable APIs
* Experimental / incubating APIs
* Internal APIs (not for users)

Do NOT document internal APIs as user-facing features.

---

## Documentation Review Checklist

Before merging documentation changes, verify:

* [ ] Matches current implementation
* [ ] Aligns with relevant RFCs
* [ ] Uses consistent terminology
* [ ] Contains runnable examples
* [ ] Explains *why*, not just *what*
* [ ] Does not expose internal abstractions

---

## Failure Conditions

Documentation changes MUST be rejected if:

* They describe behavior not implemented
* They hide breaking changes
* They contradict RFC intent
* They introduce user-facing ambiguity

---

## Core Principle

> If users need to read the source code to understand the framework,
> the documentation has failed.

Documentation exists to **eliminate guesswork**, not to summarize code.

---

This file defines the **documentation law** for Axiom.
