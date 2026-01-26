# Axiom vs Spring Boot Performance Benchmark

A+ grade fair comparison between Axiom Framework and Spring Boot 4.0.2.

## Fairness Grade: **A+**

This benchmark eliminates all known sources of bias:
- ✅ Same port (8080)
- ✅ Sequential execution (no resource competition)
- ✅ Identical endpoints (4 core tests)
- ✅ Equivalent work per request
- ✅ Same wrk configuration
- ✅ Equal warmup

See [BENCHMARK_FAIRNESS.md](BENCHMARK_FAIRNESS.md) for complete methodology.

## Test Suite (4 Tests)

1. **Hello World** — `GET /`
   - Minimal framework overhead baseline
   - Static JSON response

2. **Path Parameters** — `GET /users/:id`
   - Routing engine performance
   - Path parsing + serialization

3. **JSON Request/Response** — `POST /users`
   - JSON deserialization + object mapping
   - UUID generation + serialization

4. **Query Parameters** — `GET /search?q=test&limit=10`
   - Query string parsing
   - Type conversion + response generation

## Configuration

- **Tool:** wrk 4.2.0
- **Threads:** 8
- **Connections:** 256
- **Duration:** 60 seconds per test
- **Port:** 8080 (both frameworks)
- **Mode:** Sequential execution
| 2. Path Parameters | `GET /users/:id` | Routing performance |
| 3. JSON POST | `POST /users` | JSON parsing/serialization |

## Quick Start

### 1. Build Both Applications

```bash
# Build Axiom benchmark
cd axiom-benchmark
mvn clean package

# Build Spring Boot benchmark
cd ../spring-benchmark
mvn clean package
```

### 2. Run Benchmarks

```bash
# From benchmark directory
./run-all-benchmarks.sh > results.txt
```

The script will prompt you to:
1. Start Axiom on port 8080
2. Run Axiom tests
3. Stop Axiom
4. Start Spring Boot on port 8080
5. Run Spring Boot tests

### 3. Analyze Results

Results are saved to `results.txt` with:
- Requests per second
- Latency distribution (50th, 75th, 90th, 99th percentiles)
- Average, StdDev, Max latency
- Transfer rates

## Manual Execution

### Run Axiom

```bash
cd axiom-benchmark
java -jar target/axiom-benchmark-1.0.0.jar
```

### Run Spring Boot

```bash
cd spring-benchmark
java -jar target/spring-benchmark-1.0.0.jar
```

### Run Individual Test

```bash
# Hello World
wrk -t8 -c256 -d60s --latency http://localhost:8080/

# Path Parameters
wrk -t8 -c256 -d60s --latency http://localhost:8080/users/12345

# JSON POST
cat > /tmp/post-user.lua << 'EOF'
wrk.method = "POST"
wrk.headers["Content-Type"] = "application/json"
wrk.body = '{"name":"John Doe","email":"john@example.com"}'
EOF
wrk -t8 -c256 -d60s --latency -s /tmp/post-user.lua http://localhost:8080/users

# Query Parameters
wrk -t8 -c256 -d60s --latency "http://localhost:8080/search?q=test&limit=10"
```

## Requirements

- Java 25 (LTS)
- Maven 3.9+
- wrk 4.2.0+
- 8+ CPU cores recommended
- 4GB+ RAM minimum

## What's Measured

- **Framework overhead:** Request parsing, routing, serialization
- **Routing performance:** Path matching, parameter extraction
- **JSON handling:** Parsing, validation, serialization (both use Jackson)
- **Query processing:** String parsing, type conversion

## What's NOT Measured

- Database I/O
- Business logic complexity
- External API calls
- Heavy computation

## Fairness Checklist

✅ **Port:** Both use 8080
✅ **Execution:** Sequential (one app at a time)
✅ **Warmup:** 200 requests + 5s settle
✅ **Endpoints:** Identical across frameworks
✅ **Work:** Equivalent operations
✅ **Tool:** Same wrk parameters
✅ **Duration:** 60 seconds per test

## Removed Tests

❌ **Middleware/Protected** — Non-equivalent execution paths
- Axiom uses middleware short-circuit
- Spring Boot uses exception handler
- Unfair comparison removed

## Results Interpretation

- **Throughput:** Requests per second (higher is better)
- **P50:** Median latency (typical user experience)
- **P99:** Tail latency (worst-case user experience)
- **Max:** Absolute worst-case observed

Focus on **P99** for production readiness.

## Contributing

To maintain A+ fairness:
- Ensure all endpoints perform identical work
- Keep warmup consistent
- Run sequentially on same port
- Document any methodology changes

## License
