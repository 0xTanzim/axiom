# Known Blockers & Critical Gaps

**Last Updated:** 2026-01-12

---

## RFC Gaps (Must Address Before Implementation)

### RFC-0007: Lifecycle Management (CRITICAL)

**Status:** NOT WRITTEN
**Blocks:** Phase 3 (Runtime implementation)

**Required Content:**
- Lifecycle phases (INIT → STARTING → STARTED → STOPPING → STOPPED)
- Hook registration API
- Startup failure handling
- Graceful shutdown semantics
- In-flight request handling during shutdown
- Timeout configuration
- Error state handling

**Impact if Missing:**
- Inconsistent startup/shutdown behavior
- Resource leaks
- Undefined error states

**Action:** Draft RFC before Phase 3

---

### RFC-0008: Error Handling Architecture (HIGH)

**Status:** NOT WRITTEN
**Blocks:** Phase 1 (Error types) and Phase 3 (Error handler)

**Required Content:**
- Exception hierarchy design
- Error propagation rules
- Global error handler contract
- Error response format
- Logging integration
- Debug mode behavior
- Production mode behavior

**Impact if Missing:**
- Inconsistent error responses
- Poor debugging experience
- Security information leakage

**Action:** Draft RFC before Phase 1 completion

---

### RFC-0009: Runtime Adapter Contract (HIGH)

**Status:** NOT WRITTEN
**Blocks:** Phase 3 (JDK Runtime)

**Required Content:**
- `Server` interface definition
- `ServerConfig` parameters
- Context creation contract
- Request/response bridging rules
- Threading requirements
- Lifecycle integration
- Error propagation from adapter
- Performance requirements

**Impact if Missing:**
- Inconsistent adapter behavior
- Difficult to add new runtimes
- Performance variations

**Action:** Draft RFC before Phase 3

---

### RFC-0010: Testing Utilities API (MEDIUM)

**Status:** NOT WRITTEN
**Blocks:** Phase 4 (Testing module)

**Required Content:**
- `MockContext` design
- Test request builder API
- Response assertion API
- Integration test patterns
- Server-less testing model
- Example test patterns

**Impact if Missing:**
- Inconsistent testing patterns
- Poor test ergonomics
- Difficult to test handlers

**Action:** Draft RFC before Phase 4

---

## Technical Blockers

### Virtual Thread Pinning (Java 21)

**Status:** KNOWN ISSUE
**Severity:** Medium
**Affects:** Phase 3 (Runtime)

**Problem:**
In Java 21, virtual threads can be "pinned" to carrier threads when executing inside `synchronized` blocks with blocking operations.

**Mitigation:**
1. Use `ReentrantLock` instead of `synchronized` in framework code
2. Document pattern for users
3. Test on Java 25 where this is fixed
4. Add runtime detection and warning

**Resolution:** Java 25 LTS fixes this (target runtime)

---

### JSON Codec Performance

**Status:** POTENTIAL ISSUE
**Severity:** Low
**Affects:** Phase 5 (JSON Codecs)

**Problem:**
Jackson ObjectMapper is thread-safe but has overhead. Creating new instances is expensive.

**Mitigation:**
1. Reuse ObjectMapper instance
2. Pre-compile type information
3. Benchmark against alternatives
4. Consider Jackson afterburner module

**Resolution:** Benchmark and document best practices

---

### Module System Compatibility

**Status:** POTENTIAL ISSUE
**Severity:** Low
**Affects:** All phases

**Problem:**
JPMS module system can cause issues with:
- Reflection on internal classes
- ServiceLoader usage
- Split packages

**Mitigation:**
1. Use proper module-info.java
2. Test with and without module path
3. Document JPMS usage
4. Avoid reflection in user-facing code

**Resolution:** Careful module design, testing

---

## Architecture Concerns

### Context Thread Safety

**Status:** DESIGN DECISION NEEDED
**Severity:** Medium
**Affects:** Phase 2, Phase 3

**Problem:**
If user spawns threads from handler, should Context be thread-safe?

**Options:**
1. NOT thread-safe (document it) - Simple, performant
2. Thread-safe reads only - Balanced
3. Fully thread-safe - Complex, slower

**Recommendation:** Option 1 - NOT thread-safe, document clearly

**Resolution:** Document in RFC-0009

---

### Large Body Handling

**Status:** DESIGN DECISION NEEDED
**Severity:** Medium
**Affects:** Phase 2

**Problem:**
How to handle large request bodies without OOM?

**Options:**
1. Max body size limit (reject over limit)
2. Streaming API for large bodies
3. Temp file storage
4. Combination of above

**Recommendation:** Option 1 for v1, Option 2 for future

**Resolution:** Document limit, plan streaming API

---

### Response Streaming

**Status:** DESIGN DECISION NEEDED
**Severity:** Low
**Affects:** Phase 3

**Problem:**
Current API assumes body written at once. How to stream?

**Options:**
1. No streaming in v1 (defer)
2. Separate streaming API
3. OutputStrea access method

**Recommendation:** Option 1 for v1

**Resolution:** Defer to post-1.0 RFC

---

## External Dependencies

### Domain Name Registration

**Status:** REQUIRED
**Severity:** Low
**Affects:** Publishing (Phase 8)

**Problem:**
Maven Central requires namespace ownership verification.
Need either:
- Owned domain (axiom.io or similar)
- GitHub namespace (io.github.{org})

**Action:** Decide on namespace, verify ownership

---

### GPG Key for Signing

**Status:** REQUIRED
**Severity:** Low
**Affects:** Publishing (Phase 8)

**Problem:**
All Maven Central artifacts must be GPG signed.

**Action:** Generate GPG key, publish public key to keyserver

---

### CI/CD Secrets

**Status:** REQUIRED
**Severity:** Low
**Affects:** Publishing (Phase 8)

**Required Secrets:**
- `OSSRH_USERNAME` - Sonatype username
- `OSSRH_PASSWORD` - Sonatype password
- `GPG_PRIVATE_KEY` - Signing key
- `GPG_PASSPHRASE` - Key passphrase

**Action:** Set up secrets in GitHub repository

---

## Documentation Gaps

### Migration Guide

**Status:** NOT WRITTEN
**Severity:** Low
**Affects:** Phase 6

**Needed:**
- "From Spring Boot" guide
- "From Javalin" guide
- "From Vert.x" guide

**Action:** Write after API stable

---

### Deployment Guide

**Status:** NOT WRITTEN
**Severity:** Medium
**Affects:** Phase 6

**Needed:**
- Docker deployment
- Kubernetes deployment
- AWS Lambda (future)
- GraalVM native image (future)

**Action:** Write basic Docker guide for Phase 6

---

## Resolved Blockers

| Blocker | Resolution | Date |
|---------|------------|------|
| Architecture undocumented | ARCHITECTURE.md created | 2026-01-12 |
| Module structure unclear | MODULES.md created | 2026-01-12 |
| Roadmap missing | ROADMAP.md created | 2026-01-12 |

---

## Blocker Priority Matrix

| Priority | RFC Gaps | Technical | Architecture | External |
|----------|----------|-----------|--------------|----------|
| P0 | RFC-0007 | - | - | - |
| P1 | RFC-0008, RFC-0009 | VT Pinning | Context Safety | - |
| P2 | RFC-0010 | - | Large Body | Namespace |
| P3 | - | JSON Perf | Streaming | GPG Key |

---

*Update this document as blockers are identified and resolved.*
