package playground.di.domain;

import jakarta.validation.constraints.*;

/**
 * Login request with validation.
 */
public record LoginRequest(
    @NotBlank @Email
    String email,

    @NotBlank
    String password
) {}
