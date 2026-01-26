# Spring Boot 4.0.2 Benchmark

Fair comparison benchmark app using latest Spring Boot 4.0.2 and Java 25 LTS.

## Build

```bash
mvn clean package
```

## Run

```bash
java -jar target/spring-benchmark-1.0.0.jar
```

Server starts on `http://localhost:9001`

## Endpoints (Identical to Axiom)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Hello World |
| `/users/:id` | GET | Path parameter test |
| `/users` | POST | JSON request/response |
| `/search?q=test&limit=10` | GET | Query parameters |
| `/protected` | GET | Middleware chain (requires Authorization header) |

## Configuration

- **Port:** 9001
- **Java:** 25 LTS
- **Spring Boot:** 4.0.2
- **Logging:** WARN (minimal overhead)

## Test

```bash
# Hello World
curl http://localhost:9001/

# Path parameter
curl http://localhost:9001/users/123

# JSON POST
curl -X POST http://localhost:9001/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John","email":"john@example.com"}'

# Query parameters
curl "http://localhost:9001/search?q=test&limit=10"

# Protected endpoint
curl -H "Authorization: Bearer token" http://localhost:9001/protected
```

## Fair Benchmark Rules

✅ **Same Java version** (25 LTS)
✅ **Same endpoint logic** (identical JSON responses)
✅ **Same test duration** (30s per test)
✅ **Same concurrency** (4 threads, 100 connections)
✅ **Minimal logging** (WARN level)
✅ **No caching** (disabled)
✅ **No JMX** (disabled)

No cheating — honest comparison!
