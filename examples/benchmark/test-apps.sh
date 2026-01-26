#!/bin/bash
# Quick test script to verify both apps are working

echo "Testing Axiom and Spring Boot benchmark apps..."
echo ""

# Check if JARs exist
if [ ! -f "axiom-benchmark/target/axiom-benchmark-1.0.0.jar" ]; then
    echo "ERROR: Axiom JAR not found. Run: cd axiom-benchmark && mvn clean package"
    exit 1
fi

if [ ! -f "spring-benchmark/target/spring-benchmark-1.0.0.jar" ]; then
    echo "ERROR: Spring Boot JAR not found. Run: cd spring-benchmark && mvn clean package"
    exit 1
fi

# Start Axiom (background)
echo "Starting Axiom on port 9000..."
java -jar axiom-benchmark/target/axiom-benchmark-1.0.0.jar > /tmp/axiom-test.log 2>&1 &
AXIOM_PID=$!
sleep 3

# Start Spring Boot (background)
echo "Starting Spring Boot on port 9001..."
java -jar spring-benchmark/target/spring-benchmark-1.0.0.jar > /tmp/spring-test.log 2>&1 &
SPRING_PID=$!
sleep 5

# Test both
echo ""
echo "Testing Axiom (port 9000):"
curl -s http://localhost:9000/ && echo ""

echo ""
echo "Testing Spring Boot (port 9001):"
curl -s http://localhost:9001/ && echo ""

echo ""
echo "Both apps are running!"
echo "  - Axiom:       http://localhost:9000 (PID: $AXIOM_PID)"
echo "  - Spring Boot: http://localhost:9001 (PID: $SPRING_PID)"
echo ""
echo "To stop:"
echo "  kill $AXIOM_PID $SPRING_PID"
echo ""
echo "To run benchmarks:"
echo "  ./run-all-benchmarks.sh"
