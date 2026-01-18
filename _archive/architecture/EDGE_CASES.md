# Edge Cases & Safety Considerations

**Version:** 0.1.0-draft
**Last Updated:** 2026-01-12

---

## Overview

This document catalogs edge cases that Axiom must handle correctly.
Each section describes the scenario, expected behavior, and implementation notes.

---

## 1. Routing Edge Cases

### 1.1 Path Normalization

| Input | Normalized | Notes |
|-------|------------|-------|
| `/users/` | `/users` | Trailing slash removed |
| `//users` | `/users` | Double slash collapsed |
| `/users//123` | `/users/123` | Internal double slash collapsed |
| `` | `/` | Empty becomes root |
| `users` | `/users` | Leading slash added |
| `/users/./123` | `/users/123` | Dot segment removed |
| `/users/../admin` | `/admin` | Parent segment resolved |

**Implementation:**
- Normalize at registration time (once)
- Normalize incoming request path (once per request)
- Use consistent normalization for both

### 1.2 Route Conflicts

```java
// Conflict: same method, same path
router.get("/users/:id", handler1);
router.get("/users/:name", handler2);  // CONFLICT - both are params

// Not conflict: different param names but same position is OK
// Framework uses position, not name
```

**Behavior:**
- Detect conflicts at registration time
- Throw `RouteConflictException` with clear message
- Include both conflicting routes in error

### 1.3 Param Edge Cases

| Path Pattern | Request Path | Extracted Params |
|--------------|--------------|------------------|
| `/users/:id` | `/users/123` | `{id: "123"}` |
| `/users/:id` | `/users/` | 404 (no match) |
| `/users/:id` | `/users` | 404 (no match) |
| `/users/:id/posts/:pid` | `/users/1/posts/2` | `{id: "1", pid: "2"}` |
| `/files/*` | `/files/a/b/c` | `{*: "a/b/c"}` |

### 1.4 Wildcard Behavior

```java
router.get("/files/*", handler);

// Matches:
// /files/a
// /files/a/b/c
// /files/

// Does NOT match:
// /files (no trailing content)
```

**Rule:** Wildcard requires at least empty segment after.

### 1.5 Method Case Sensitivity

```java
// Request method is case-insensitive (HTTP spec)
// "GET", "get", "Get" all match GET handler
```

### 1.6 Path Case Sensitivity

```java
// Paths are case-sensitive (default)
// /Users != /users

// Future: configurable case sensitivity
```

### 1.7 URL Encoding

| Request Path | Decoded | Matched Pattern |
|--------------|---------|-----------------|
| `/users/John%20Doe` | `/users/John Doe` | `/users/:name` |
| `/users/%2F` | `/users//` | Tricky - escaped slash |

**Rule:** Decode after splitting, not before.

---

## 2. Context Edge Cases

### 2.1 Body Read Multiple Times

```java
// First read
User user = c.body(User.class);

// Second read - must return same result
User user2 = c.body(User.class);

// Different type - must fail or cache bytes
String raw = c.body(String.class);  // Should work
```

**Implementation:**
- Cache raw bytes on first read
- Deserialize on demand
- Same type returns cached object

### 2.2 Body Parse Failure

```java
// Invalid JSON
c.body(User.class);  // throws BodyParseException

// BodyParseException includes:
// - Original content (truncated)
// - Expected type
// - Parse error message
```

### 2.3 Missing Parameters

```java
// Param not in path
c.param("nonexistent");  // returns null, not exception

// Query not in URL
c.query("page");  // returns null
c.query("page", "1");  // returns default "1"
```

### 2.4 Response Already Committed

```java
c.text("Hello");  // Response committed

c.status(500);  // throws ResponseCommittedException
c.header("X-Custom", "value");  // throws ResponseCommittedException
c.json(error);  // throws ResponseCommittedException
```

### 2.5 Response Write Order

```java
// CORRECT order
c.status(201);
c.header("X-Custom", "value");
c.json(response);  // commits

// WRONG order - headers after body
c.json(response);  // commits
c.header("X-Custom", "value");  // throws!
```

### 2.6 State Type Safety

```java
// Set state
c.set("user", currentUser);

// Get with correct type
User u = c.get("user", User.class).orElse(null);

// Get with wrong type
Admin a = c.get("user", Admin.class);  // ClassCastException or empty?
```

**Decision:** Return `Optional.empty()` on type mismatch, log warning.

### 2.7 Null Values

```java
// Null body
c.json(null);  // Writes "null" JSON

// Null status?
c.status(0);  // Invalid - throw IllegalArgumentException

// Null header value
c.header("X-Custom", null);  // Remove header or throw?
```

**Decision:** `null` header value removes the header.

---

## 3. Middleware Edge Cases

### 3.1 Next Called Multiple Times

```java
app.use((c, next) -> {
    next.run();
    next.run();  // Second call - what happens?
});
```

**Options:**
1. Throw exception on second call
2. No-op on second call
3. Re-execute chain (dangerous)

**Decision:** Throw `IllegalStateException` - next can only be called once.

### 3.2 Next Never Called

```java
app.use((c, next) -> {
    if (!authorized) {
        c.status(401);
        c.text("Unauthorized");
        return;  // next NOT called - intentional short-circuit
    }
    next.run();
});
```

**Behavior:** Valid use case. Chain stops, response is written.

### 3.3 Exception in Middleware

```java
app.use((c, next) -> {
    throw new RuntimeException("oops");
});
```

**Behavior:**
1. Exception propagates up
2. Global error handler catches it
3. Returns 500 (or custom error response)

### 3.4 Exception After Next

```java
app.use((c, next) -> {
    next.run();
    throw new RuntimeException("oops");  // After handler succeeded
});
```

**Behavior:**
- If response committed: Log error, cannot change response
- If response not committed: Error handler runs

### 3.5 Middleware Order

```java
app.use(middleware1);  // Runs first (outer)
app.use(middleware2);  // Runs second (inner)
app.use(middleware3);  // Runs third (innermost)

// Execution order:
// → middleware1 before
//   → middleware2 before
//     → middleware3 before
//       → handler
//     ← middleware3 after
//   ← middleware2 after
// ← middleware1 after
```

### 3.6 Router-Specific Middleware

```java
Router admin = new Router();
admin.use(adminAuthMiddleware);  // Only for admin routes

app.route("/admin", admin);
```

**Behavior:** Middleware scoped to router, not global.

---

## 4. Lifecycle Edge Cases

### 4.1 Startup Failure

```java
app.onStart(() -> {
    throw new RuntimeException("DB connection failed");
});

app.listen(8080);  // What happens?
```

**Behavior:**
1. Exception logged
2. Startup hooks rolled back (if any)
3. `StartupException` thrown from `listen()`
4. Application does not start

### 4.2 Shutdown During Request

```java
// Request in progress
// SIGTERM received

// Behavior:
// 1. Stop accepting new connections
// 2. Wait for in-flight requests (timeout)
// 3. After timeout, force close
// 4. Run shutdown hooks
// 5. Exit
```

### 4.3 Graceful Shutdown Timeout

```java
ServerConfig config = ServerConfig.builder()
    .shutdownTimeout(Duration.ofSeconds(30))
    .build();

// If requests don't complete in 30s, force close
```

### 4.4 Double Start

```java
app.listen(8080);
app.listen(9090);  // Already running!
```

**Behavior:** Throw `IllegalStateException` - app already running.

### 4.5 Stop When Not Running

```java
app.stop();  // Never started
```

**Behavior:** No-op or throw? **Decision:** No-op, idempotent.

### 4.6 Start After Stop

```java
app.listen(8080);
app.stop();
app.listen(8080);  // Restart?
```

**Behavior:** Allow restart. Reset internal state.

---

## 5. Concurrency Edge Cases

### 5.1 Context Across Threads

```java
handler = c -> {
    Thread.startVirtualThread(() -> {
        c.json(result);  // Different thread!
    });
};
```

**Problem:** Context is not thread-safe.

**Solutions:**
1. Document: "Don't share context across threads"
2. Detect and throw
3. Make context thread-safe (performance cost)

**Decision:** Document, don't enforce (trust users).

### 5.2 Virtual Thread Pinning

```java
// Java 21: synchronized blocks pin carrier thread
synchronized(lock) {
    someBlockingIO();  // BAD in Java 21
}

// Use ReentrantLock instead
lock.lock();
try {
    someBlockingIO();  // OK
} finally {
    lock.unlock();
}
```

**Note:** Java 25 fixes this, but document for Java 21 users.

### 5.3 ThreadLocal Usage

```java
// DON'T use ThreadLocal with virtual threads
ThreadLocal<User> currentUser;  // BAD

// Use Context state instead
c.set("user", user);  // GOOD

// Or ScopedValue (Java 21+)
ScopedValue<User> currentUser;  // GOOD
```

### 5.4 Request Timeout

```java
handler = c -> {
    Thread.sleep(60000);  // 1 minute
};

// Should framework enforce timeout?
```

**Decision:** Configurable request timeout at server level.

---

## 6. HTTP Edge Cases

### 6.1 Large Headers

```java
// Request with 1MB header value
// What happens?
```

**Behavior:** Runtime adapter limit. Framework should have configurable max.

### 6.2 Large Body

```java
// 1GB file upload
// What happens?
```

**Behavior:**
- Configurable max body size (default: 10MB)
- Exceed limit → 413 Payload Too Large
- Streaming API for large files (future)

### 6.3 Malformed Request

```java
// Invalid HTTP/1.1 request
// Missing Host header
// Invalid Content-Length
```

**Behavior:** Runtime adapter handles. Framework sees valid requests only.

### 6.4 Content-Type Mismatch

```java
// Content-Type: application/json
// Body: not valid JSON
c.body(User.class);  // BodyParseException
```

### 6.5 Missing Content-Type

```java
// No Content-Type header
c.body(User.class);  // Assume JSON? Or fail?
```

**Decision:** Configurable default, JSON by default.

### 6.6 Empty Body

```java
// POST with empty body
c.body(User.class);  // BodyParseException? Or null?
```

**Decision:** Return `null` for empty body (optional handling).

---

## 7. Error Handling Edge Cases

### 7.1 Error in Error Handler

```java
app.onError((c, e) -> {
    throw new RuntimeException("Error handler failed");
});
```

**Behavior:**
1. Log original error
2. Log error handler failure
3. Send minimal 500 response
4. Do not recurse

### 7.2 Error After Response Committed

```java
handler = c -> {
    c.text("Starting...");  // Response committed
    throw new RuntimeException("Oops");
};
```

**Behavior:**
- Cannot change response (already sent)
- Log error
- Connection may be in bad state (close it)

### 7.3 OutOfMemoryError

```java
handler = c -> {
    throw new OutOfMemoryError();  // Fatal!
};
```

**Behavior:**
- Don't catch `Error` in normal flow
- Let it propagate
- JVM will handle (crash or GC)

---

## 8. Testing Edge Cases

### 8.1 Mock Context Behavior

```java
MockContext c = MockContext.get("/users");
c.json(response);  // Store for assertion
c.json(response2);  // What happens?
```

**Behavior:** In test context, allow multiple writes for inspection.

### 8.2 Async Handler Testing

```java
handler = c -> {
    Thread.startVirtualThread(() -> {
        c.json(result);
    });
};

// Test completes before async work
```

**Solution:** Provide `TestContext.await()` method.

---

## 9. Configuration Edge Cases

### 9.1 Invalid Port

```java
app.listen(-1);    // Invalid
app.listen(99999); // Invalid
app.listen(80);    // May need root
```

**Behavior:** Validate port range (1-65535), throw `ConfigurationException`.

### 9.2 Port Already in Use

```java
app.listen(8080);  // Another process using 8080
```

**Behavior:** `StartupException` with clear message about port conflict.

### 9.3 Environment Variables

```java
// PORT env var common in containers
int port = Integer.parseInt(System.getenv("PORT"));
app.listen(port);

// What if PORT is not set? null!
```

**Recommendation:** Document env var patterns, provide helpers.

---

## 10. Security Edge Cases

### 10.1 Path Traversal

```java
router.get("/files/:name", c -> {
    String name = c.param("name");  // "../../../etc/passwd"
    // User must validate!
});
```

**Framework Responsibility:**
- Document the risk
- Provide `PathUtils.sanitize()` helper
- Don't automatically serve files

### 10.2 Header Injection

```java
c.header("X-Custom", userInput);  // userInput contains \r\n
```

**Behavior:** Validate header values, strip or reject CRLF.

### 10.3 JSON Injection

```java
c.json(Map.of("name", userInput));
// userInput: "</script><script>evil()</script>"
```

**Behavior:** JSON encoding handles this. But document XSS risks.

### 10.4 Request Smuggling

**Behavior:** Runtime adapter responsibility. Test with various adapters.

---

## Summary Matrix

| Category | Edge Cases | Priority |
|----------|------------|----------|
| Routing | 7 | P0 |
| Context | 7 | P0 |
| Middleware | 6 | P0 |
| Lifecycle | 6 | P1 |
| Concurrency | 4 | P1 |
| HTTP | 6 | P1 |
| Error Handling | 3 | P0 |
| Testing | 2 | P2 |
| Configuration | 3 | P2 |
| Security | 4 | P0 |

---

*This document should be referenced during implementation and testing.*
