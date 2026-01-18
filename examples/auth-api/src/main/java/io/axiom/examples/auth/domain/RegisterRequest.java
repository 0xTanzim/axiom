package io.axiom.examples.auth.domain;

/**
 * Registration request payload.
 */
public record RegisterRequest(
    String username,
    String email,
    String password
) {
    public boolean isValid() {
        return username != null && !username.isBlank()
            && email != null && email.contains("@")
            && password != null && password.length() >= 6;
    }
}
