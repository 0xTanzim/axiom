package playground.di.routes;

import io.axiom.core.routing.Router;
import io.axiom.di.Routes;
import io.axiom.validation.AxiomValidator;
import jakarta.inject.Inject;
import playground.di.domain.*;
import playground.di.service.AuthService;
import playground.di.service.AuthService.AuthResult;

import java.util.Map;

/**
 * Authentication routes: login, register, logout, me.
 *
 * @Routes("/auth") — auto-mounted at /auth
 *
 * Endpoints:
 *   POST /auth/login    — Authenticate user
 *   POST /auth/register — Create new account
 *   POST /auth/logout   — Invalidate token
 *   GET  /auth/me       — Get current user from token
 */
@Routes("/auth")
public class AuthRoutes {

    private final AuthService authService;

    @Inject
    public AuthRoutes(AuthService authService) {
        this.authService = authService;
    }

    public Router router() {
        Router r = new Router();

        // POST /auth/login
        r.post("/login", ctx -> {
            var request = ctx.body(LoginRequest.class);

            // Validate input
            var validation = AxiomValidator.validate(request);
            if (!validation.isValid()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "error", "Validation failed",
                    "details", validation.errors()
                ));
                return;
            }

            // Authenticate
            var result = authService.login(request.email(), request.password());

            switch (result) {
                case AuthResult.Success success -> {
                    ctx.json(success.token());
                }
                case AuthResult.Failure failure -> {
                    ctx.status(401);
                    ctx.json(Map.of("error", failure.message()));
                }
            }
        });

        // POST /auth/register
        r.post("/register", ctx -> {
            var request = ctx.body(RegisterRequest.class);

            // Validate input
            var validation = AxiomValidator.validate(request);
            if (!validation.isValid()) {
                ctx.status(400);
                ctx.json(Map.of(
                    "error", "Validation failed",
                    "details", validation.errors()
                ));
                return;
            }

            // Register
            var result = authService.register(request);

            switch (result) {
                case AuthResult.Success success -> {
                    ctx.status(201);
                    ctx.json(success.token());
                }
                case AuthResult.Failure failure -> {
                    int status = failure.isConflict() ? 409 : 400;
                    ctx.status(status);
                    ctx.json(Map.of("error", failure.message()));
                }
            }
        });

        // POST /auth/logout
        r.post("/logout", ctx -> {
            String token = extractToken(ctx.header("Authorization"));
            if (token != null) {
                authService.logout(token);
            }
            ctx.status(204);
            ctx.text("");
        });

        // GET /auth/me
        r.get("/me", ctx -> {
            String token = extractToken(ctx.header("Authorization"));

            if (token == null) {
                ctx.status(401);
                ctx.json(Map.of("error", "Missing authorization header"));
                return;
            }

            var userOpt = authService.validateToken(token);

            if (userOpt.isEmpty()) {
                ctx.status(401);
                ctx.json(Map.of("error", "Invalid or expired token"));
                return;
            }

            ctx.json(userOpt.get().toResponse());
        });

        return r;
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
}
