# Axiom Implementation Roadmap

**Version:** 0.1.0-draft
**Last Updated:** 2026-01-12

---

## Overview

This roadmap defines the phased implementation plan for Axiom.
Each phase builds on the previous one with clear deliverables.

---

## Phase Summary

| Phase | Name | Focus | Duration |
|-------|------|-------|----------|
| 0 | Foundation | Project setup, RFCs | 1-2 weeks |
| 1 | Core Engine | Handler, Router, Middleware | 3-4 weeks |
| 2 | HTTP Layer | HTTP types, Context | 2-3 weeks |
| 3 | JDK Runtime | First working server | 2-3 weeks |
| 4 | Testing | Test utilities, examples | 2 weeks |
| 5 | JSON Codecs | Jackson, Gson integration | 1-2 weeks |
| 6 | Documentation | User guide, API docs | 2 weeks |
| 7 | Performance | Benchmarks, optimization | 2 weeks |
| 8 | Alpha Release | 0.1.0-alpha | 1 week |
| 9 | Netty Runtime | High-performance adapter | 3-4 weeks |
| 10 | Beta Release | 0.1.0-beta | 1 week |
| 11 | Polish | Feedback, fixes | 2-3 weeks |
| 12 | GA Release | 0.1.0 | 1 week |

**Total estimated time:** 6-8 months to first stable release

---

## Phase 0: Foundation (Current)

### Status: IN PROGRESS

### Goals
- Complete project setup
- Finalize architecture documentation
- Complete all critical RFCs

### Deliverables
- [ ] Architecture documentation
- [ ] Module structure definition
- [ ] RFC-0007: Lifecycle Management
- [ ] RFC-0008: Error Handling
- [ ] RFC-0009: Runtime Adapter Contract
- [ ] RFC-0010: Testing Utilities
- [ ] Gradle multi-module setup
- [ ] CI/CD pipeline (GitHub Actions)
- [ ] Code style guide

### Exit Criteria
- All core RFCs accepted
- Build system functional
- CI running

---

## Phase 1: Core Engine

### Goals
- Implement fundamental abstractions
- Zero external dependencies
- Foundation for all other modules

### Deliverables
- [ ] `Handler` interface
- [ ] `Context` interface (abstract)
- [ ] `Router` with route registration
- [ ] `Segment` sealed hierarchy
- [ ] `RouteTrie` implementation
- [ ] `Middleware` internal model
- [ ] `MiddlewareHandler` public API
- [ ] `Next` continuation
- [ ] Middleware composition
- [ ] Unit tests (90%+ coverage)

### API Surface
```java
// Handler
@FunctionalInterface
interface Handler {
    void handle(Context c) throws Exception;
}

// Router
Router router = new Router();
router.get("/path", handler);
router.post("/path", handler);
router.group("/api", r -> { ... });

// Middleware
app.use((c, next) -> {
    // before
    next.run();
    // after
});
```

### Exit Criteria
- All core abstractions implemented
- Unit tests passing
- No external dependencies

---

## Phase 2: HTTP Layer

### Goals
- HTTP-specific types
- Body parsing contracts
- Concrete Context implementation

### Deliverables
- [ ] `HttpMethod` enum
- [ ] `HttpStatus` constants
- [ ] `Headers` utilities
- [ ] `ContentType` handling
- [ ] `BodyParser` interface
- [ ] `JsonCodec` interface
- [ ] `HttpContext` implementation
- [ ] Request data extraction
- [ ] Response writing
- [ ] Unit tests

### API Surface
```java
// HTTP types
HttpMethod.GET
HttpStatus.OK
ContentType.JSON

// Context usage
c.method()  // "GET"
c.path()    // "/users/123"
c.param("id")  // "123"
c.query("page")  // "1"
c.body(User.class)  // deserialized
c.json(response)  // serialized
```

### Exit Criteria
- HTTP abstractions complete
- Body parsing working
- Context fully implemented

---

## Phase 3: JDK Runtime

### Goals
- First working HTTP server
- JDK HttpServer integration
- Virtual threads enabled

### Deliverables
- [ ] `Server` interface in core
- [ ] `ServerConfig` record
- [ ] `JdkServer` implementation
- [ ] `JdkContext` adapter
- [ ] Virtual thread executor
- [ ] Lifecycle management
- [ ] Error handling
- [ ] Integration tests
- [ ] Hello World example

### API Surface
```java
App app = new JdkApp();
app.use(loggingMiddleware);
app.route(router);
app.listen(8080);

// Hello World works!
```

### Exit Criteria
- Server starts and handles requests
- Routes work
- Middleware chain works
- Graceful shutdown

---

## Phase 4: Testing Utilities

### Goals
- Enable serverless testing
- Mock context implementation
- Assertion helpers

### Deliverables
- [ ] `MockContext` implementation
- [ ] `TestRouter` utilities
- [ ] `TestApp` for integration tests
- [ ] Custom assertions
- [ ] Example test patterns
- [ ] Documentation

### API Surface
```java
// Unit test (no server)
Context c = MockContext.get("/users/123");
handler.handle(c);
assertThat(c).hasStatus(200);
assertThat(c).hasJsonBody(user);

// Integration test
TestApp app = TestApp.create();
Response r = app.get("/users/123");
assertThat(r).isOk();
```

### Exit Criteria
- Handlers testable without server
- Clear testing patterns documented

---

## Phase 5: JSON Codecs

### Goals
- Jackson integration
- Gson integration
- Pluggable codec system

### Deliverables
- [ ] `JacksonCodec` implementation
- [ ] `GsonCodec` implementation
- [ ] Codec registration API
- [ ] Content negotiation
- [ ] Error handling for parse failures
- [ ] Performance testing

### API Surface
```java
// Jackson
app.codec(new JacksonCodec());

// Then JSON works
c.body(User.class);  // Jackson deserializes
c.json(user);        // Jackson serializes
```

### Exit Criteria
- Both codecs working
- Performance acceptable
- Easy to add new codecs

---

## Phase 6: Documentation

### Goals
- User-facing documentation
- API reference
- Migration guides

### Deliverables
- [ ] README.md (comprehensive)
- [ ] Getting Started guide
- [ ] Routing guide
- [ ] Middleware guide
- [ ] Testing guide
- [ ] Deployment guide
- [ ] API Javadoc
- [ ] VitePress site setup

### Exit Criteria
- New users can start with docs alone
- API fully documented
- Examples for common patterns

---

## Phase 7: Performance

### Goals
- Establish baselines
- Optimize hot paths
- Validate architecture

### Deliverables
- [ ] JMH benchmark suite
- [ ] Routing benchmarks
- [ ] Handler throughput benchmarks
- [ ] Memory profiling
- [ ] Optimization pass
- [ ] Performance documentation

### Targets
| Operation | Target |
|-----------|--------|
| Route match | < 100ns |
| Middleware (3) | < 500ns |
| Hello world | < 10μs |
| Throughput | 100K+ req/s |

### Exit Criteria
- Benchmarks documented
- Targets met
- No obvious bottlenecks

---

## Phase 8: Alpha Release (0.1.0-alpha)

### Goals
- First public release
- Gather feedback
- Identify issues

### Deliverables
- [ ] Maven Central publication
- [ ] Release notes
- [ ] Migration guide (from nothing)
- [ ] Feedback channels setup
- [ ] Issue templates

### API Stability
- APIs may change
- Breaking changes expected
- Experimental label

### Exit Criteria
- Artifacts on Maven Central
- Announcement posted
- Feedback mechanism ready

---

## Phase 9: Netty Runtime

### Goals
- High-performance adapter
- Production-grade server
- Advanced features

### Deliverables
- [ ] `NettyServer` implementation
- [ ] `NettyContext` adapter
- [ ] HTTP/1.1 support
- [ ] HTTP/2 support (stretch)
- [ ] SSL/TLS support
- [ ] Performance comparison vs JDK

### Exit Criteria
- Netty adapter functional
- Performance better than JDK
- Documentation complete

---

## Phase 10: Beta Release (0.1.0-beta)

### Goals
- API stabilization
- Bug fixes from alpha
- Feature complete

### Deliverables
- [ ] API freeze (no breaks after)
- [ ] Bug fixes
- [ ] Documentation updates
- [ ] Migration guide updates

### API Stability
- APIs frozen
- Only bug fixes
- Deprecations documented

### Exit Criteria
- No known critical bugs
- Documentation complete
- Community feedback addressed

---

## Phase 11: Polish

### Goals
- Final refinements
- Edge case handling
- Production hardening

### Deliverables
- [ ] Edge case fixes
- [ ] Error message improvements
- [ ] Performance tuning
- [ ] Security review
- [ ] Final documentation pass

### Exit Criteria
- Production ready
- Security reviewed
- All issues triaged

---

## Phase 12: GA Release (0.1.0)

### Goals
- First stable release
- Production recommended
- Long-term support begins

### Deliverables
- [ ] Final release artifacts
- [ ] Changelog
- [ ] Announcement
- [ ] Support policy
- [ ] Future roadmap

### API Stability
- Stable API
- SemVer applies
- Breaking changes = major version

### Exit Criteria
- Released to Maven Central
- Announced publicly
- Support channels active

---

## Future Phases (Post 0.1.0)

### 0.2.0 - Extensions
- Validation module
- OpenAPI generation
- Metrics/tracing

### 0.3.0 - Advanced Features
- WebSocket support
- Server-Sent Events
- HTTP/3 (experimental)

### 1.0.0 - Mature Release
- API stability guarantee
- Long-term support
- Enterprise features

---

## Risk Register

| Risk | Impact | Mitigation |
|------|--------|------------|
| Virtual thread issues | High | Test on Java 21, 23, 25 |
| API instability | Medium | Careful design review |
| Performance regression | Medium | Continuous benchmarking |
| JSON codec perf | Low | Profile and optimize |
| Documentation gaps | Medium | Early doc writing |

---

## Success Metrics

| Metric | Target |
|--------|--------|
| Time to Hello World | < 5 minutes |
| Core module size | < 50KB |
| Routing throughput | 100K+ req/s |
| Test coverage | > 90% |
| Documentation coverage | 100% public API |
| GitHub stars (year 1) | 500+ |
| Maven downloads (year 1) | 10K+ |

---

*This roadmap is a living document. Adjust as needed based on feedback and learnings.*
