package io.axiom.examples.auth.routes;

import io.axiom.core.routing.Router;
import io.axiom.di.Routes;
import io.axiom.examples.auth.domain.*;
import io.axiom.examples.auth.service.AuthService;
import io.axiom.examples.auth.service.AuthService.AuthResult;
import jakarta.inject.Inject;

/**
 * Authentication routes: login, register, logout.
 *
 * <p>Demonstrates Axiom's clean handler pattern:
 * - Request body parsing with c.body(Class)
 * - JSON responses with c.json(Object)
 * - Status codes with c.status(int)
 */
@Routes("/auth")
public class AuthRoutes {

    private final AuthService authService;

    @Inject
    public AuthRoutes(AuthService authService) {
        this.authService = authService;
    }

    public Router router() {
        var router = new Router();

        // POST /auth/login - Authenticate user
        router.post("/login", ctx -> {
            var request = ctx.body(LoginRequest.class);
            var result = authService.login(request);

            switch (result) {
                case AuthResult.Success success -> {
                    ctx.status(200);
                    ctx.json(success.token());
                }
                case AuthResult.Failure failure -> {
                    ctx.status(401);
                    ctx.json(ErrorResponse.unauthorized(failure.message()));
                }
            }
        });

        // POST /auth/register - Create new user
        router.post("/register", ctx -> {
            var request = ctx.body(RegisterRequest.class);
            var result = authService.register(request);

            switch (result) {
                case AuthResult.Success success -> {
                    ctx.status(201);
                    ctx.json(success.token());
                }
                case AuthResult.Failure failure -> {
                    int status = failure.isConflict() ? 409 : 400;
                    ctx.status(status);
                    ctx.json(failure.isConflict()
                        ? ErrorResponse.conflict(failure.message())
                        : ErrorResponse.badRequest(failure.message()));
                }
            }
        });

        // POST /auth/logout - Invalidate token
        router.post("/logout", ctx -> {
            var token = extractToken(ctx.header("Authorization"));
            if (token != null) {
                authService.logout(token);
            }
            ctx.status(204);
            ctx.text("");
        });

        // GET /auth/me - Get current user from token
        router.get("/me", ctx -> {
            var token = extractToken(ctx.header("Authorization"));
            if (token == null) {
                ctx.status(401);
                ctx.json(ErrorResponse.unauthorized("Missing authorization header"));
                return;
            }

            var userOpt = authService.validateToken(token);
            if (userOpt.isEmpty()) {
                ctx.status(401);
                ctx.json(ErrorResponse.unauthorized("Invalid or expired token"));
                return;
            }

            ctx.json(userOpt.get().toResponse());
        });

        return router;
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
}
