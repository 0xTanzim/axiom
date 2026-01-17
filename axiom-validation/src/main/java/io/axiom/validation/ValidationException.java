package io.axiom.validation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Exception thrown when validation fails.
 *
 * <p>Contains structured validation errors that can be used to build
 * HTTP error responses.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * try {
 *     User user = AxiomValidator.validateOrThrow(request);
 * } catch (ValidationException e) {
 *     // Get structured errors for JSON response
 *     ctx.status(400);
 *     ctx.json(Map.of("errors", e.errors()));
 *
 *     // Or get as error map
 *     Map<String, List<String>> errorMap = e.toErrorMap();
 *     // { "email": ["must not be blank", "must be valid email"] }
 * }
 * }</pre>
 */
public class ValidationException extends RuntimeException {

    private final List<ValidationError> errors;

    /**
     * Creates a validation exception with errors.
     *
     * @param errors the validation errors
     */
    public ValidationException(List<ValidationError> errors) {
        super(buildMessage(errors));
        Objects.requireNonNull(errors, "errors must not be null");
        if (errors.isEmpty()) {
            throw new IllegalArgumentException("ValidationException must have at least one error");
        }
        this.errors = List.copyOf(errors);
    }

    /**
     * Creates a validation exception with a single error.
     *
     * @param error the validation error
     */
    public ValidationException(ValidationError error) {
        this(List.of(error));
    }

    /**
     * Creates a validation exception with path and message.
     *
     * @param path the property path
     * @param message the error message
     */
    public ValidationException(String path, String message) {
        this(new ValidationError(path, message));
    }

    /**
     * Returns the list of validation errors.
     *
     * @return immutable list of errors
     */
    public List<ValidationError> errors() {
        return errors;
    }

    /**
     * Returns errors as a map from field path to list of messages.
     *
     * <p>Useful for building JSON error responses:
     * <pre>{@code
     * {
     *   "email": ["must not be blank"],
     *   "age": ["must be at least 18", "must be a positive number"]
     * }
     * }</pre>
     *
     * @return map of path to error messages
     */
    public Map<String, List<String>> toErrorMap() {
        return errors.stream()
                .collect(Collectors.groupingBy(
                        ValidationError::path,
                        HashMap::new,
                        Collectors.mapping(ValidationError::message, Collectors.toList())
                ));
    }

    /**
     * Returns the number of validation errors.
     *
     * @return error count
     */
    public int errorCount() {
        return errors.size();
    }

    private static String buildMessage(List<ValidationError> errors) {
        if (errors == null || errors.isEmpty()) {
            return "Validation failed";
        }
        if (errors.size() == 1) {
            return "Validation failed: " + errors.getFirst();
        }
        return "Validation failed with %d errors: %s".formatted(
                errors.size(),
                errors.stream()
                        .map(ValidationError::toString)
                        .collect(Collectors.joining("; "))
        );
    }
}
