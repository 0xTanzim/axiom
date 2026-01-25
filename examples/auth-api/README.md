# Axiom Auth API Example

A complete authentication API demonstrating Axiom framework's DX patterns:
- **Login/Register** with token-based auth
- **Data retrieval** with CRUD operations
- **Compile-time DI** with Dagger 2
- **Clean handler patterns** with `c.body()`, `c.json()`, `c.status()`

## Project Structure

```
auth-api/
├── src/main/java/io/axiom/examples/auth/
│   ├── Application.java           # Entry point
│   ├── di/
│   │   └── AppComponent.java      # Dagger component
│   ├── domain/                    # DTOs & records
│   │   ├── User.java
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   └── ...
│   ├── repository/
│   │   └── UserRepository.java    # In-memory data store
│   ├── service/
│   │   ├── AuthService.java       # Login/register logic
│   │   └── UserService.java       # User CRUD
│   └── routes/
│       ├── AuthRoutes.java        # /auth endpoints
│       ├── UserRoutes.java        # /users endpoints
│       └── HealthRoutes.java      # /health endpoints
└── pom.xml
```

## Running

**All Platforms:**
```bash
cd examples/auth-api
mvn compile exec:java
```

Server starts at `http://localhost:8080`

## API Endpoints

### Health

**Linux/macOS:**
```bash
# Health check
curl http://localhost:8080/health

# Readiness probe
curl http://localhost:8080/health/ready

# Liveness probe
curl http://localhost:8080/health/live
```

**Windows (PowerShell):**
```powershell
# Health check
curl http://localhost:8080/health

# Readiness probe
curl http://localhost:8080/health/ready

# Liveness probe
curl http://localhost:8080/health/live
```

### Authentication

**Linux/macOS:**
```bash
# Register new user
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@example.com","password":"secret123"}'

# Login (returns token)
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"secret123"}'

# Get current user (requires token)
curl http://localhost:8080/auth/me \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# Logout
curl -X POST http://localhost:8080/auth/logout \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

**Windows (PowerShell):**
```powershell
# Register new user
Invoke-RestMethod -Method POST -Uri http://localhost:8080/auth/register `
  -ContentType "application/json" `
  -Body '{"username":"john","email":"john@example.com","password":"secret123"}'

# Login (returns token)
Invoke-RestMethod -Method POST -Uri http://localhost:8080/auth/login `
  -ContentType "application/json" `
  -Body '{"username":"john","password":"secret123"}'

# Get current user (requires token)
Invoke-RestMethod -Uri http://localhost:8080/auth/me `
  -Headers @{"Authorization"="Bearer YOUR_TOKEN_HERE"}

# Logout
Invoke-RestMethod -Method POST -Uri http://localhost:8080/auth/logout `
  -Headers @{"Authorization"="Bearer YOUR_TOKEN_HERE"}
```

### Users

**Linux/macOS:**
```bash
# List all users
curl http://localhost:8080/users

# Get user by ID
curl http://localhost:8080/users/123456789

# Get user by username
curl http://localhost:8080/users/by-username/demo

# Delete user
curl -X DELETE http://localhost:8080/users/123456789

# User statistics
curl http://localhost:8080/users/stats
```

**Windows (PowerShell):**
```powershell
# List all users
curl http://localhost:8080/users

# Get user by ID
curl http://localhost:8080/users/123456789

# Get user by username
curl http://localhost:8080/users/by-username/demo

# Delete user
curl -Method DELETE http://localhost:8080/users/123456789

# User statistics
curl http://localhost:8080/users/stats
```

## DX Patterns Demonstrated

### 1. Clean Handler Pattern

```java
router.post("/login", ctx -> {
    var request = ctx.body(LoginRequest.class);  // Parse JSON body
    var result = authService.login(request);

    switch (result) {
        case AuthResult.Success s -> {
            ctx.status(200);
            ctx.json(s.token());  // Send JSON response
        }
        case AuthResult.Failure f -> {
            ctx.status(401);
            ctx.json(ErrorResponse.unauthorized(f.message()));
        }
    }
});
```

### 2. Path Parameters

```java
router.get("/:id", ctx -> {
    var id = Long.parseLong(ctx.param("id"));  // Extract path param
    var user = userService.findById(id);
    ctx.json(user.orElseThrow());
});
```

### 3. Compile-Time DI

```java
@Service
public class AuthService {
    @Inject
    public AuthService(UserRepository repo) {  // Auto-wired by Dagger
        this.userRepository = repo;
    }
}
```

### 4. Auto-Route Discovery

```java
@Routes("/auth")  // Mounted at /auth automatically
public class AuthRoutes {
    @Inject
    public AuthRoutes(AuthService authService) { ... }

    public Router router() { ... }
}
```

### 5. Application Startup

```java
// ONE LINE - Framework handles everything!
AxiomApplication.start(Application.class, 8080);
```

## Demo User

A demo user is pre-created:
- Username: `demo`
- Password: `demo123`

**Linux/macOS:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo123"}'
```

**Windows (PowerShell):**
```powershell
Invoke-RestMethod -Method POST -Uri http://localhost:8080/auth/login `
  -ContentType "application/json" `
  -Body '{"username":"demo","password":"demo123"}'
```
