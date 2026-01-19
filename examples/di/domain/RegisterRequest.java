package playground.di.domain;

import jakarta.validation.constraints.*;

/**
 * Registration request with validation.
 */
public record RegisterRequest(
    @NotBlank @Size(min = 2, max = 100)
    String name,

    @NotBlank @Email
    String email,

    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
    String password
) {}
