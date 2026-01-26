#!/bin/bash
# Axiom vs Spring Boot Performance Benchmark
# Run all wrk tests and generate report
# FAIR BENCHMARK: Sequential execution on same port
# FULLY AUTOMATIC: No manual intervention required

set -e

# Trap to ensure cleanup on exit
trap cleanup EXIT INT TERM

cleanup() {
    echo ""
    echo "Cleaning up processes..."
    pkill -f "axiom-benchmark-1.0.0.jar" 2>/dev/null || true
    pkill -f "spring-benchmark-1.0.0.jar" 2>/dev/null || true
    rm -f /tmp/post-user.lua /tmp/with-auth.lua
}

echo "=================================================="
echo "   Axiom vs Spring Boot Performance Benchmark"
echo "   A+ GRADE FAIRNESS: Sequential, Same Port"
echo "   FULLY AUTOMATIC EXECUTION"
echo "=================================================="
echo ""
echo "Configuration:"
echo "  - Threads: 8"
echo "  - Connections: 256"
echo "  - Duration: 60s per test"
echo "  - Tool: wrk"
echo "  - Port: 8080 (both frameworks)"
echo "  - Mode: Sequential (one app at a time)"
echo ""

# Check if wrk is installed
if ! command -v wrk &> /dev/null; then
    echo "ERROR: wrk is not installed"
    echo "Install: sudo apt-get install wrk"
    exit 1
fi

# Check if jar files exist
if [ ! -f "axiom-benchmark/target/axiom-benchmark-1.0.0.jar" ]; then
    echo "ERROR: Axiom benchmark jar not found"
    echo "Run: cd axiom-benchmark && mvn clean package"
    exit 1
fi

if [ ! -f "spring-benchmark/target/spring-benchmark-1.0.0.jar" ]; then
    echo "ERROR: Spring Boot benchmark jar not found"
    echo "Run: cd spring-benchmark && mvn clean package"
    exit 1
fi

# Function to wait for app to be ready
wait_for_app() {
    local app_name=$1
    echo "Waiting for $app_name to start on port 8080..." >&2
    for i in {1..60}; do
        if curl -s --max-time 2 http://localhost:8080/ > /dev/null 2>&1; then
            echo "✓ $app_name is ready" >&2
            return 0
        fi
        echo -n "." >&2
        sleep 1
    done
    echo "" >&2
    echo "ERROR: $app_name failed to start within 60 seconds" >&2
    exit 1
}

# Function to warmup
warmup_app() {
    local app_name=$1
    echo "Warming up $app_name..." >&2
    for i in {1..100}; do
        curl -s --max-time 2 http://localhost:8080/ > /dev/null 2>&1 || true
        curl -s --max-time 2 http://localhost:8080/users/12345 > /dev/null 2>&1 || true
    done
    sleep 3
    echo "✓ Warmup complete" >&2
}

# Function to stop app on port 8080
stop_app() {
    local app_name=$1
    local jar_name=$2
    echo "Stopping $app_name..." >&2

    # Kill by jar name (more reliable)
    pkill -f "$jar_name" 2>/dev/null || true
    sleep 2

    # Force kill if still running
    pkill -9 -f "$jar_name" 2>/dev/null || true
    sleep 1

    # Verify port is free
    if lsof -ti:8080 > /dev/null 2>&1; then
        local pid=$(lsof -ti:8080)
        kill -9 $pid 2>/dev/null || true
        sleep 1
    fi

    echo "✓ $app_name stopped" >&2
}

# ========================================
# AXIOM BENCHMARKS
# ========================================

echo "=================================================="
echo "PART 1: AXIOM FRAMEWORK"
echo "=================================================="
echo ""

# Start Axiom
echo "Starting Axiom..." >&2
cd axiom-benchmark
nohup java -jar target/axiom-benchmark-1.0.0.jar > /tmp/axiom.log 2>&1 &
AXIOM_PID=$!
cd ..
echo "✓ Axiom started (PID: $AXIOM_PID)" >&2
sleep 2

wait_for_app "Axiom"
warmup_app "Axiom"

echo ""
echo "=================================================="
echo "Test 1: Hello World (GET /)"
echo "=================================================="
echo ""
wrk -t8 -c256 -d60s --latency http://localhost:8080/
echo ""
sleep 3

echo "=================================================="
echo "Test 2: Path Parameters (GET /users/:id)"
echo "=================================================="
echo ""
wrk -t8 -c256 -d60s --latency http://localhost:8080/users/12345
echo ""
sleep 3

echo "=================================================="
echo "Test 3: JSON Request/Response (POST /users)"
echo "=================================================="
echo ""
cat > /tmp/post-user.lua << 'EOF'
wrk.method = "POST"
wrk.headers["Content-Type"] = "application/json"
wrk.body = '{"name":"John Doe","email":"john@example.com"}'
EOF

wrk -t8 -c256 -d60s --latency -s /tmp/post-user.lua http://localhost:8080/users
echo ""
sleep 3

echo "=================================================="
echo "Test 4: Query Parameters (GET /search?q=test&limit=10)"
echo "=================================================="
echo ""
wrk -t8 -c256 -d60s --latency "http://localhost:8080/search?q=test&limit=10"
echo ""

echo "=================================================="
echo "AXIOM BENCHMARKS COMPLETE"
echo "=================================================="
echo ""

stop_app "Axiom" "axiom-benchmark-1.0.0.jar"
sleep 2

# ========================================
# SPRING BOOT BENCHMARKS
# ========================================

echo ""
echo "=================================================="
echo "PART 2: SPRING BOOT 4.0.2"
echo "=================================================="
echo ""

# Start Spring Boot
echo "Starting Spring Boot..." >&2
cd spring-benchmark
nohup java -jar target/spring-benchmark-1.0.0.jar > /tmp/spring.log 2>&1 &
SPRING_PID=$!
cd ..
echo "✓ Spring Boot started (PID: $SPRING_PID)" >&2
sleep 2

wait_for_app "Spring Boot"
warmup_app "Spring Boot"

echo ""
echo "=================================================="
echo "Test 1: Hello World (GET /)"
echo "=================================================="
echo ""
wrk -t8 -c256 -d60s --latency http://localhost:8080/
echo ""
sleep 3

echo "=================================================="
echo "Test 2: Path Parameters (GET /users/:id)"
echo "=================================================="
echo ""
wrk -t8 -c256 -d60s --latency http://localhost:8080/users/12345
echo ""
sleep 3

echo "=================================================="
echo "Test 3: JSON Request/Response (POST /users)"
echo "=================================================="
echo ""
wrk -t8 -c256 -d60s --latency -s /tmp/post-user.lua http://localhost:8080/users
echo ""
sleep 3

echo "=================================================="
echo "Test 4: Query Parameters (GET /search?q=test&limit=10)"
echo "=================================================="
echo ""
wrk -t8 -c256 -d60s --latency "http://localhost:8080/search?q=test&limit=10"
echo ""

echo "=================================================="
echo "   ALL BENCHMARKS COMPLETE"
echo "=================================================="
echo ""
echo "FAIRNESS CHECKLIST:"
echo "  ✓ Same port (8080)"
echo "  ✓ Sequential execution (no competition)"
echo "  ✓ Equal warmup"
echo "  ✓ Same wrk parameters"
echo "  ✓ Identical endpoints"
echo "  ✓ Equivalent work per request"
echo ""

# Stop Spring Boot
stop_app "Spring Boot" "spring-benchmark-1.0.0.jar"

echo "=================================================="
echo "   BENCHMARK COMPLETE - FULLY AUTOMATIC"
echo "=================================================="
echo ""
echo "Results saved to stdout (redirect with > results.txt)"
echo ""
