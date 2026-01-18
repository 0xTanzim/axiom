---
description: "Write Axiom documentation strictly from the current codebase and implemented RFCs. There are no legacy docs. Documentation must be modern, code-first, and reflect real, verified behavior."
name: "Axiom_Framework_Docs_Writer"
agent: Axiom_Framework_Engineer
---

# Write Axiom Docs From Code (Strict, Greenfield, Modern)

## Mission

Generate **Axiom documentation** that is **fully synchronized with the current implementation** and presented using **modern documentation UX**.

Documentation is derived from:
- Actual source code
- Verified runtime behavior
- RFCs **only where implementation exists**

There are **no legacy docs**.
There is **no backward compatibility burden**.

If it’s not in code, it does not exist.

---

## Core Documentation Principle (Very Important)

> **Axiom supports two first-class usage styles.
Documentation must present them side-by-side on the same page.**

### The Two Styles

1. **Modern (Axiom DI)**
   - `Axiom.start(...)`
   - `@Routes`, `@Service`, `@Repository`
   - Compile-time DI (Dagger wrapper)
   - Auto-mounting and zero-config bootstrap

2. **Core (Manual)**
   - Explicit `App`, `Router`, and wiring
   - No annotations
   - No DI
   - Maximum control and transparency

❗ Neither style is legacy
❗ Neither style is hidden
❗ Modern (DI) is shown first, Core second

---

## Docs Scope & Location (Important)

- All documentation lives under: `docs/content/docs/`
- Documentation files are **MDX**, not plain Markdown
- Each docs page maps to **one real module or subsystem**
- Examples in `/examples` are **references**, not documentation

❌ Do not duplicate examples
❌ Do not paste full example apps into docs

---

## Hard Rules (Non-Negotiable)

❌ Never write docs before inspecting code
❌ Never guess behavior, defaults, or options
❌ Never invent APIs or features
❌ Never document unimplemented RFC sections
❌ Never assume future plans
❌ Never add diagrams without verifying execution flow
❌ Never split styles into separate pages

If required context is missing → **STOP and ask**.

---

## Required Inputs

- `${input:targetDocPath}` — Path under `docs/content/docs/`
- `${input:packageName}` — Target module (e.g. `axiom-core`, `axiom-persistence`)

Optional:
- `${input:focusArea}` — Specific API or subsystem

Missing required input → **halt immediately**.

---

## Mandatory Workflow (Order Is Enforced)

### Step 1 — Inspect the Codebase First (Always)

Inspect the exact module being documented:
- `axiom-core`
- `axiom-persistence`
- validation
- runtime (JDK server)

Read:
- Public APIs
- Defaults and constants
- Error paths
- Lifecycle and threading behavior

If behavior is unclear → dig deeper or stop.

---

### Step 2 — Inspect RFCs (Context Only)

RFCs provide **intent**, not truth.

- Only document RFC sections **implemented in code**
- RFCs never override source code

---

### Step 3 — Decide on Documentation Layout (Tabs Gate)

For every feature:

- Exists in both styles → **Tabs required**
- Exists in only one style → **state this explicitly**

Mandatory pattern:

```mdx
<Tabs>
<Tab title="Modern (Axiom DI)">
</Tab>
<Tab title="Core (Manual)">
</Tab>
</Tabs>
```

---

### Step 4 — Decide on Diagrams (Mermaid Gate)

Add diagrams **only** if they explain:

* Request lifecycle
* Middleware pipeline
* Routing flow
* Adapter interaction
* Lifecycle transitions

No speculation. No decoration.

---

### Step 5 — Write the Documentation

* Exact API names only
* Real defaults only
* Runnable snippets only
* No future-facing language
* Show both styles side-by-side when applicable

Follow:

* `docs.instructions.md`

---

### Step 6 — Validation Pass (Mandatory)

* [ ] API exists
* [ ] Examples compile
* [ ] Defaults match code
* [ ] Tabs are correct
* [ ] Diagrams reflect execution
* [ ] No speculative language

---

## Failure Conditions (Stop Immediately)

Stop if:

* Code cannot be found
* Behavior is ambiguous
* RFC contradicts implementation
* Feature is not implemented
* Required inputs are missing

---

## Output Expectations

* Write to `${input:targetDocPath}`
* MDX only
* Tabs where applicable
* Runnable code
* Neutral, precise language

---

## Final Directive

> **Axiom documentation is a mirror of the code — nothing more, nothing less.**

Accuracy is mandatory.
Clarity is expected.
Guessing is forbidden.
