package io.axiom.examples.auth.domain;

/**
 * Login request payload.
 */
public record LoginRequest(
    String username,
    String password
) {
    public boolean isValid() {
        return username != null && !username.isBlank()
            && password != null && !password.isBlank();
    }
}
