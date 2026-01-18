package com.example.auth.routes;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.service.AuthService;
import io.axiom.core.routing.Router;
import io.axiom.validation.AxiomValidator;
import io.axiom.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Authentication routes: registration and login.
 *
 * <p>These routes are public (no authentication required).
 */
public class AuthRoutes {

    private static final Logger LOG = LoggerFactory.getLogger(AuthRoutes.class);

    private final AuthService authService;

    public AuthRoutes(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Creates the router with auth routes.
     */
    public Router router() {
        Router router = new Router();

        // POST /auth/register
        router.post("/register", ctx -> {
            RegisterRequest request = ctx.body(RegisterRequest.class);

            // Validate input
            ValidationResult<RegisterRequest> validation = AxiomValidator.validate(request);
            if (!validation.isValid()) {
                ctx.status(400);
                ctx.json(Map.of(
                        "error", "Validation failed",
                        "details", validation.errors()
                ));
                return;
            }

            // Attempt registration
            var response = authService.register(request);
            if (response.isEmpty()) {
                ctx.status(409);  // Conflict
                ctx.json(Map.of(
                        "error", "Email already registered"
                ));
                return;
            }

            ctx.status(201);
            ctx.json(response.get());
        });

        // POST /auth/login
        router.post("/login", ctx -> {
            LoginRequest request = ctx.body(LoginRequest.class);

            // Validate input
            ValidationResult<LoginRequest> validation = AxiomValidator.validate(request);
            if (!validation.isValid()) {
                ctx.status(400);
                ctx.json(Map.of(
                        "error", "Validation failed",
                        "details", validation.errors()
                ));
                return;
            }

            // Attempt login
            var response = authService.login(request);
            if (response.isEmpty()) {
                ctx.status(401);
                ctx.json(Map.of(
                        "error", "Invalid email or password"
                ));
                return;
            }

            ctx.json(response.get());
        });

        return router;
    }
}
