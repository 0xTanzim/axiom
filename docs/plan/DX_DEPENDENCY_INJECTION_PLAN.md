# DX Improvement Plan: Dependency Injection in Axiom

## Status: See RFC-0011

> **Full specification: [RFC-0011: Axiom DI](../../draft/RFC_0011.md)**

---

## The Vision: Express/Hono-Level DX

```java
// THE ENTIRE APPLICATION
public class Application {
    public static void main(String[] args) {
        Axiom.start(8080);  // That's it!
    }
}
```

**Everything else is automatic:**
- Services wired at compile time
- Routes auto-mounted
- Config auto-injected
- Zero boilerplate

---

## Key Annotations

```java
@Service      // Business logic
@Repository   // Data access
@Routes       // HTTP handlers → AUTO-MOUNTED!
```

---

## How It Works

```java
// 1. Define your repository
@Repository
public class UserRepository {
    public List<User> findAll() {
        return Jdbc.queryList("SELECT * FROM users", ...);
    }
}

// 2. Define your service
@Service
public class UserService {
    private final UserRepository repo;
    public UserService(UserRepository repo) { this.repo = repo; }
}

// 3. Define your routes (auto-mounted at /users!)
@Routes("/users")
public class UserRoutes {
    private final UserService service;
    public UserRoutes(UserService service) { this.service = service; }

    public Router router() {
        Router r = new Router();
        r.get("/", c -> c.json(service.findAll()));
        return r;
    }
}

// 4. Start the app
public class Application {
    public static void main(String[] args) {
        Axiom.start(8080);  // Routes auto-registered!
    }
}
```

---

## Before vs After

| Aspect | Before (Manual) | After (Axiom DI) |
|--------|-----------------|------------------|
| Application.java | 25+ lines | **5 lines** |
| Services.java | 50+ lines | **Generated** |
| Route registration | Manual per route | **Auto** |
| Config passing | Everywhere | **Auto-injected** |

---

## Cycle Resolution

```java
// Circular: A → B → A
// Fix: Use Lazy<T>
@Service
public class ServiceA {
    public ServiceA(Lazy<ServiceB> b) { }  // Breaks cycle
}
```

---

## See Also

- [RFC-0011: Full Specification](../../draft/RFC_0011.md)
- [RFC-0002: Routing Design](../../draft/RFC_0002.md)
