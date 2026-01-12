# Axiom Framework — Immediate TODO

**Updated:** January 12, 2026
**Next Sprint:** JDK HttpServer Runtime Adapter

---

## 🎯 P0: JDK Runtime Adapter (NEXT)

**Goal:** Make `app.listen(8080)` actually work

**Status:** Not started
**Effort:** 2-3 days
**Blocking:** All example apps, demos, real usage

### Tasks

- [ ] **Create Maven module** `axiom-runtime-jdk`
  - Add `pom.xml` with dependency on `axiom-core`
  - Add `module-info.java` with exports
  - Configure compiler for Java 25

- [ ] **Implement JdkServer**
  - Wrap `com.sun.net.httpserver.HttpServer`
  - Support host/port binding
  - Virtual thread executor configuration
  - Graceful shutdown logic

- [ ] **Implement JdkContextBridge**
  - Convert `HttpExchange` → `DefaultContext.Request`
  - Convert `DefaultContext.Response` → `HttpExchange.sendResponseHeaders/Body`
  - Handle path params injection
  - Handle query params parsing
  - Handle headers mapping
  - Handle request body reading
  - Handle response body writing

- [ ] **Implement JdkServerApp**
  - Extend `DefaultApp`
  - Override `listen(host, port)` to start server
  - Override `stop()` to shutdown gracefully
  - Override `port()` to return bound port
  - Override `isRunning()` for server state

- [ ] **Write Tests**
  - Integration test: Start server, make HTTP call, verify response
  - Test middleware execution order
  - Test error handling (404, 405, 500)
  - Test graceful shutdown

- [ ] **Create Example App**
  - `examples/hello-jdk/` directory
  - Simple "Hello World" with JDK runtime
  - README with run instructions

---

## 📝 P1: Documentation Updates

- [ ] Update main `README.md` with JDK runtime example
- [ ] Write "Getting Started" guide
- [ ] Document runtime adapter contract (RFC-0009)
- [ ] Add architecture diagram showing core + runtime separation

---

## 🔬 P2: Write Missing RFCs

- [ ] **RFC-0007:** Lifecycle Management
  - Startup/shutdown state machine
  - Hook registration API
  - Graceful shutdown timeout
  - Error state handling

- [ ] **RFC-0008:** Error Handling Architecture
  - Exception hierarchy design
  - Global error handler contract
  - Debug vs production modes
  - Stack trace handling

- [ ] **RFC-0009:** Runtime Adapter Contract
  - `Server` interface definition
  - Context bridging specification
  - Threading requirements
  - Performance expectations

- [ ] **RFC-0010:** Testing Utilities
  - `MockContext` design
  - Test request/response builders
  - Assertion API

---

## 🧪 P3: Example Applications

Create `examples/` directory:

- [ ] **hello-world** — Single GET endpoint
- [ ] **json-api** — CRUD with JSON responses
- [ ] **middleware-chain** — Auth + logging + CORS
- [ ] **nested-routers** — Modular route organization
- [ ] **error-pages** — Custom 404/500 pages

---

## 🚀 P4: Developer Experience

- [ ] Startup banner with framework version
- [ ] Better error messages (file + line numbers)
- [ ] Development mode with auto-reload
- [ ] Request logging middleware
- [ ] Response time metrics middleware
- [ ] CORS middleware
- [ ] Static file serving middleware

---

## ⚡ P5: Performance & Production

- [ ] Benchmark JDK runtime vs Netty vs others
- [ ] Add performance tests
- [ ] Optimize hot paths (routing, middleware composition)
- [ ] Add metrics collection
- [ ] Create Netty runtime adapter (optional)

---

## 📦 P6: Packaging & Release

- [ ] Create Maven parent POM
- [ ] Configure Maven Central publishing
- [ ] Write CHANGELOG
- [ ] Tag version 0.1.0
- [ ] Publish to Maven Central
- [ ] Announce on Reddit/HN

---

## Current Focus (This Week)

**Day 1-2:** JDK Runtime Implementation
- Create module structure
- Implement HttpServer bridge
- Get basic "Hello World" working

**Day 3:** Testing & Examples
- Write integration tests
- Create hello-world example
- Verify middleware works end-to-end

**Day 4:** Documentation
- Update README
- Write Getting Started guide
- Document runtime adapter pattern

**Day 5:** Polish & Release Prep
- Fix any bugs found
- Clean up code
- Prepare for first demo

---

## How to Track Progress

Update this file as tasks complete:
- [ ] Incomplete
- [x] Complete

Mark with date when done:
- [x] Task name — ✅ 2026-01-12

---

## Questions to Resolve

1. **Virtual Threads:** Use by default or make it configurable?
   - Recommendation: Default ON for Java 21+, OFF for older versions

2. **Error Responses:** JSON by default or HTML?
   - Recommendation: JSON for API routes, HTML for regular routes (content negotiation)

3. **Shutdown Timeout:** What's a good default?
   - Recommendation: 30 seconds (configurable)

4. **Port Selection:** Allow port 0 for random port?
   - Recommendation: Yes, useful for testing

---

**Next Action:** Create `axiom-runtime-jdk` module and start implementing JdkServer. 🚀
