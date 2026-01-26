package com.example.springbenchmark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Spring Boot 4.0.2 benchmark application for fair comparison with Axiom.
 * Identical endpoints and logic to Axiom benchmark.
 */
@SpringBootApplication
@RestController
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // Test 1: Hello World (minimal overhead)
    @GetMapping("/")
    public Map<String, String> hello() {
        return Map.of("message", "Hello, World!");
    }

    // Test 2: Path Parameters
    @GetMapping("/users/{id}")
    public Map<String, String> getUser(@PathVariable String id) {
        return Map.of(
                "id", id,
                "name", "User " + id
        );
    }

    // Test 3: JSON Request/Response
    @PostMapping("/users")
    public Map<String, String> createUser(@RequestBody CreateUserRequest request) {
        return Map.of(
                "id", UUID.randomUUID().toString(),
                "name", request.name(),
                "email", request.email()
        );
    }

    // Test 4: Query Parameters
    @GetMapping("/search")
    public Map<String, Object> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        return Map.of(
                "query", q,
                "limit", limit,
                "results", List.of()
        );
    }

    record CreateUserRequest(String name, String email) {
    }
}
