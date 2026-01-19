package playground.manual;

import io.axiom.core.app.*;
import io.axiom.core.routing.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Complete CRUD application without DI annotations.
 *
 * Demonstrates:
 * - Manual dependency wiring
 * - Full CRUD operations
 * - Pagination
 * - Search/filtering
 * - Error handling
 * - Response DTOs
 *
 * Test:
 *   # List all users
 *   curl http://localhost:8080/users
 *
 *   # List with pagination
 *   curl "http://localhost:8080/users?page=1&size=10"
 *
 *   # Search
 *   curl "http://localhost:8080/users?search=john"
 *
 *   # Get one user
 *   curl http://localhost:8080/users/1
 *
 *   # Create user
 *   curl -X POST http://localhost:8080/users \
 *     -H "Content-Type: application/json" \
 *     -d '{"name":"John Doe","email":"john@example.com"}'
 *
 *   # Update user
 *   curl -X PUT http://localhost:8080/users/1 \
 *     -H "Content-Type: application/json" \
 *     -d '{"name":"John Updated","email":"john.updated@example.com"}'
 *
 *   # Delete user
 *   curl -X DELETE http://localhost:8080/users/1
 */
public class CrudApp {

    public static void main(String[] args) {
        // ===========================================
        // MANUAL DEPENDENCY WIRING
        // ===========================================

        UserRepository userRepository = new UserRepository();
        UserService userService = new UserService(userRepository);

        // Seed some data
        userService.create(new CreateUserRequest("Alice Smith", "alice@example.com"));
        userService.create(new CreateUserRequest("Bob Jones", "bob@example.com"));
        userService.create(new CreateUserRequest("Charlie Brown", "charlie@example.com"));

        // ===========================================
        // CREATE APP
        // ===========================================

        App app = Axiom.create();

        // Logging middleware
        app.use((ctx, next) -> {
            long start = System.currentTimeMillis();
            System.out.println("→ " + ctx.method() + " " + ctx.path());
            next.run();
            System.out.println("← " + ctx.method() + " " + ctx.path() + " (" + (System.currentTimeMillis() - start) + "ms)");
        });

        // ===========================================
        // USER ROUTES
        // ===========================================

        Router userRouter = new Router();

        // GET /users - List with pagination and search
        userRouter.get("/", ctx -> {
            int page = ctx.queryInt("page", 1);
            int size = ctx.queryInt("size", 10);
            String search = ctx.query("search", "");

            List<UserResponse> users = search.isEmpty()
                ? userService.findAll(page, size)
                : userService.search(search, page, size);

            long total = search.isEmpty()
                ? userService.count()
                : userService.countSearch(search);

            ctx.json(new PagedResponse<>(users, page, size, total));
        });

        // GET /users/:id - Get single user
        userRouter.get("/:id", ctx -> {
            long id = Long.parseLong(ctx.paramOrThrow("id"));

            userService.findById(id).ifPresentOrElse(
                user -> ctx.json(user),
                () -> {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("User not found", "USER_NOT_FOUND"));
                }
            );
        });

        // POST /users - Create user
        userRouter.post("/", ctx -> {
            CreateUserRequest request = ctx.body(CreateUserRequest.class);

            // Simple validation
            if (request.name() == null || request.name().isBlank()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("Name is required", "VALIDATION_ERROR"));
                return;
            }
            if (request.email() == null || request.email().isBlank()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("Email is required", "VALIDATION_ERROR"));
                return;
            }

            // Check for duplicate email
            if (userService.existsByEmail(request.email())) {
                ctx.status(409);
                ctx.json(new ErrorResponse("Email already exists", "DUPLICATE_EMAIL"));
                return;
            }

            UserResponse created = userService.create(request);
            ctx.status(201);
            ctx.json(created);
        });

        // PUT /users/:id - Update user
        userRouter.put("/:id", ctx -> {
            long id = Long.parseLong(ctx.paramOrThrow("id"));
            UpdateUserRequest request = ctx.body(UpdateUserRequest.class);

            try {
                UserResponse updated = userService.update(id, request);
                ctx.json(updated);
            } catch (NotFoundException e) {
                ctx.status(404);
                ctx.json(new ErrorResponse(e.getMessage(), "USER_NOT_FOUND"));
            }
        });

        // DELETE /users/:id - Delete user
        userRouter.delete("/:id", ctx -> {
            long id = Long.parseLong(ctx.paramOrThrow("id"));

            if (!userService.delete(id)) {
                ctx.status(404);
                ctx.json(new ErrorResponse("User not found", "USER_NOT_FOUND"));
                return;
            }

            ctx.status(204);
            ctx.text("");  // No content
        });

        // ===========================================
        // HEALTH ROUTE
        // ===========================================

        Router healthRouter = new Router();
        healthRouter.get("/", ctx -> {
            ctx.json(Map.of(
                "status", "healthy",
                "users", userService.count(),
                "timestamp", System.currentTimeMillis()
            ));
        });

        // ===========================================
        // MOUNT & START
        // ===========================================

        app.route("/users", userRouter);
        app.route("/health", healthRouter);

        // Error handler
        app.onError((ctx, error) -> {
            if (error instanceof NumberFormatException) {
                ctx.status(400);
                ctx.json(new ErrorResponse("Invalid ID format", "INVALID_ID"));
                return;
            }

            System.err.println("Unhandled error: " + error.getMessage());
            error.printStackTrace();
            ctx.status(500);
            ctx.json(new ErrorResponse("Internal server error", "INTERNAL_ERROR"));
        });

        System.out.println("Starting CrudApp at http://localhost:8080");
        System.out.println("Seeded with 3 users");
        app.listen(8080);
    }

    // ===========================================
    // DOMAIN CLASSES
    // ===========================================

    record User(long id, String name, String email, long createdAt) {
        UserResponse toResponse() {
            return new UserResponse(id, name, email, createdAt);
        }
    }

    // ===========================================
    // REQUEST/RESPONSE DTOs
    // ===========================================

    record CreateUserRequest(String name, String email) {}
    record UpdateUserRequest(String name, String email) {}
    record UserResponse(long id, String name, String email, long createdAt) {}
    record ErrorResponse(String message, String code) {}
    record PagedResponse<T>(List<T> data, int page, int size, long total) {
        public int totalPages() {
            return (int) Math.ceil((double) total / size);
        }
        public boolean hasNext() {
            return page < totalPages();
        }
        public boolean hasPrevious() {
            return page > 1;
        }
    }

    // ===========================================
    // REPOSITORY (Data Access Layer)
    // ===========================================

    static class UserRepository {
        private final Map<Long, User> users = new ConcurrentHashMap<>();
        private final AtomicLong idGenerator = new AtomicLong(0);

        public User save(User user) {
            if (user.id() == 0) {
                // Insert
                long id = idGenerator.incrementAndGet();
                User newUser = new User(id, user.name(), user.email(), System.currentTimeMillis());
                users.put(id, newUser);
                return newUser;
            } else {
                // Update
                users.put(user.id(), user);
                return user;
            }
        }

        public Optional<User> findById(long id) {
            return Optional.ofNullable(users.get(id));
        }

        public List<User> findAll() {
            return List.copyOf(users.values());
        }

        public List<User> search(String query) {
            String lowerQuery = query.toLowerCase();
            return users.values().stream()
                .filter(u -> u.name().toLowerCase().contains(lowerQuery)
                          || u.email().toLowerCase().contains(lowerQuery))
                .toList();
        }

        public boolean existsByEmail(String email) {
            return users.values().stream()
                .anyMatch(u -> u.email().equalsIgnoreCase(email));
        }

        public boolean delete(long id) {
            return users.remove(id) != null;
        }

        public long count() {
            return users.size();
        }
    }

    // ===========================================
    // SERVICE (Business Logic Layer)
    // ===========================================

    static class UserService {
        private final UserRepository repository;

        UserService(UserRepository repository) {
            this.repository = repository;
        }

        public UserResponse create(CreateUserRequest request) {
            User user = new User(0, request.name(), request.email(), 0);
            return repository.save(user).toResponse();
        }

        public UserResponse update(long id, UpdateUserRequest request) {
            User existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

            User updated = new User(
                id,
                request.name() != null ? request.name() : existing.name(),
                request.email() != null ? request.email() : existing.email(),
                existing.createdAt()
            );
            return repository.save(updated).toResponse();
        }

        public Optional<UserResponse> findById(long id) {
            return repository.findById(id).map(User::toResponse);
        }

        public List<UserResponse> findAll(int page, int size) {
            return repository.findAll().stream()
                .skip((long) (page - 1) * size)
                .limit(size)
                .map(User::toResponse)
                .toList();
        }

        public List<UserResponse> search(String query, int page, int size) {
            return repository.search(query).stream()
                .skip((long) (page - 1) * size)
                .limit(size)
                .map(User::toResponse)
                .toList();
        }

        public boolean delete(long id) {
            return repository.delete(id);
        }

        public long count() {
            return repository.count();
        }

        public long countSearch(String query) {
            return repository.search(query).size();
        }

        public boolean existsByEmail(String email) {
            return repository.existsByEmail(email);
        }
    }

    // ===========================================
    // EXCEPTION
    // ===========================================

    static class NotFoundException extends RuntimeException {
        NotFoundException(String message) {
            super(message);
        }
    }
}
