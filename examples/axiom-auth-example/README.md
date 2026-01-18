# Axiom Auth Example

A complete authentication example demonstrating:

- User registration & login
- Password hashing (BCrypt)
- JWT token generation & validation
- Protected routes with middleware
- Database persistence (H2/PostgreSQL)
- Input validation
- Configuration management
- Hot reload for development

## Project Structure

```
axiom-auth-example/
├── pom.xml                          # Maven configuration
├── .env                             # Environment variables (dev)
├── application.properties           # Default config
├── src/main/java/
│   └── com/example/auth/
│       ├── Application.java         # Entry point
│       ├── config/
│       │   └── AppConfig.java       # Type-safe config interface
│       ├── domain/
│       │   └── User.java            # User entity
│       ├── dto/
│       │   ├── RegisterRequest.java # Registration input
│       │   ├── LoginRequest.java    # Login input
│       │   └── AuthResponse.java    # Token response
│       ├── repository/
│       │   └── UserRepository.java  # Database access
│       ├── service/
│       │   ├── AuthService.java     # Authentication logic
│       │   └── JwtService.java      # JWT handling
│       ├── middleware/
│       │   └── AuthMiddleware.java  # Token validation
│       └── routes/
│           ├── AuthRoutes.java      # /auth/* endpoints
│           └── UserRoutes.java      # /users/* (protected)
└── src/test/java/
    └── com/example/auth/
        └── AuthIntegrationTest.java # E2E tests
```

## Quick Start

```bash
# Run with hot reload (dev mode)
mvn compile exec:java -Dexec.mainClass=com.example.auth.Application

# Or with JBang (single file dev)
jbang Application.java
```

## API Endpoints

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| POST | /auth/register | Create new user | No |
| POST | /auth/login | Get JWT token | No |
| GET | /users/me | Get current user | Yes |
| PUT | /users/me | Update profile | Yes |

## Configuration

Environment variables or `application.properties`:

```properties
# Server
server.port=8080
server.host=0.0.0.0

# Database
database.url=jdbc:h2:mem:auth;DB_CLOSE_DELAY=-1
database.username=sa
database.password=

# JWT
jwt.secret=your-256-bit-secret-key-here
jwt.expiration=86400
```

## Flow Diagram

```
┌─────────┐    POST /auth/register     ┌─────────────┐
│ Client  │ ───────────────────────────▶│ AuthRoutes  │
└─────────┘                             └──────┬──────┘
     │                                         │
     │                                         ▼
     │                                  ┌─────────────┐
     │                                  │ Validation  │
     │                                  └──────┬──────┘
     │                                         │
     │                                         ▼
     │                                  ┌─────────────┐
     │                                  │ AuthService │
     │                                  └──────┬──────┘
     │                                         │
     │                                         ▼
     │    POST /auth/login              ┌─────────────────┐
     │ ─────────────────────────────────▶│ UserRepository │
     │                                  └─────────────────┘
     │                                         │
     │                                         ▼
     │                                  ┌─────────────┐
     │    { token: "eyJ..." }           │  JwtService │
     │ ◀─────────────────────────────────└─────────────┘
     │
     │    GET /users/me
     │    Authorization: Bearer eyJ...
     │ ─────────────────────────────────▶┌────────────────┐
     │                                   │ AuthMiddleware │
     │                                   └───────┬────────┘
     │                                           │ ctx.attribute("userId", ...)
     │                                           ▼
     │    { id: 1, email: "..." }        ┌─────────────┐
     │ ◀──────────────────────────────────│ UserRoutes  │
     │                                   └─────────────┘
```
