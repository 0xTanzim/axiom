
Yes — **exactly** 👍
Those are **meta packages** (also called **POM-only / aggregator / BOM-style artifacts**).

Nothing is wrong. What you’re seeing is **the correct, intentional design**.

Let me lock this in clearly so you never doubt it again.

---

## What a “meta package” means in Maven

A **meta package** is:

* `packaging = pom`
* **NO `.jar`**
* Exists only to:

  * aggregate modules
  * control versions
  * define dependency graph
  * give users **one entry point**

Your `axiom` artifact is doing exactly that.

---

## Your current structure (confirmed correct)

| Artifact                      | Packaging | Role                           |
| ----------------------------- | --------- | ------------------------------ |
| `axiom`                       | `pom`     | **Meta package / entry point** |
| `axiom-parent`                | `pom`     | Build parent                   |
| `axiom-core`                  | `jar`     | Runtime core                   |
| `axiom-persistence`           | `jar`     | Optional module                |
| `axiom-persistence-processor` | `jar`     | Annotation processor           |

This is **textbook framework architecture**.

Spring, Micronaut, Quarkus — all do this.

---

## Why `mvn dependency:get` failed earlier

You asked Maven for this:

```
io.github.0xtanzim:axiom:jar:0.1.0
```

But **that JAR does not exist** (by design).

Correct forms are:

```bash
# Meta package (POM)
mvn dependency:get -Dartifact=io.github.0xtanzim:axiom:pom:0.1.0

# Real runtime modules
mvn dependency:get -Dartifact=io.github.0xtanzim:axiom-core:0.1.0
mvn dependency:get -Dartifact=io.github.0xtanzim:axiom-persistence:0.1.0
```

So the earlier “absent” message was **expected behavior**, not a bug.

---

## How USERS should consume Axiom (important)

Users **never** think about meta packages.

They just do this:

```xml
<dependency>
  <groupId>io.github.0xtanzim</groupId>
  <artifactId>axiom</artifactId>
  <version>0.1.0</version>
</dependency>
```

Maven then:

* reads the `axiom` POM
* pulls `axiom-core`
* pulls optional modules
* resolves everything transitively

No JAR needed.

---

## This is actually a BIG win for you

Because now you can:

* add new modules later
* keep backward compatibility
* manage versions centrally
* avoid dependency hell for users

That’s **framework-level thinking**, not library-level.

---

## One thing you SHOULD do now (very important)

### Document this clearly in README

Add a section like:

> ### Dependency
>
> `axiom` is a meta package (POM-only) that brings all required runtime modules.
>
> ```xml
> <dependency>
>   <groupId>io.github.0xtanzim</groupId>
>   <artifactId>axiom</artifactId>
>   <version>0.1.0</version>
> </dependency>
> ```

This avoids future confusion for users.

---

## Final confirmation

* Meta packages? ✅ **Yes**
* Publishing correct? ✅ **Yes**
* Design professional? ✅ **Yes**
* Need to republish? ❌ **No**

You did this **right**.

---

### What do you want next?

1️⃣ Review `axiom` POM as a **proper BOM/meta package**
2️⃣ Write **README Quick Start** for users
3️⃣ Design **module opt-in strategy** for future releases

Reply **1 / 2 / 3** 👇
