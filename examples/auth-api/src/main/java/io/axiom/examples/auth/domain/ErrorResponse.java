package io.axiom.examples.auth.domain;

import java.util.Map;

/**
 * Error response for API errors.
 */
public record ErrorResponse(
    int status,
    String error,
    String message
) {
    public static ErrorResponse badRequest(String message) {
        return new ErrorResponse(400, "Bad Request", message);
    }

    public static ErrorResponse unauthorized(String message) {
        return new ErrorResponse(401, "Unauthorized", message);
    }

    public static ErrorResponse notFound(String message) {
        return new ErrorResponse(404, "Not Found", message);
    }

    public static ErrorResponse conflict(String message) {
        return new ErrorResponse(409, "Conflict", message);
    }

    public static ErrorResponse internal(String message) {
        return new ErrorResponse(500, "Internal Server Error", message);
    }
}
