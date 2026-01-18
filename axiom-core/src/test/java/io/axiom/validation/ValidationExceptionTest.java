package io.axiom.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ValidationException.
 */
@DisplayName("ValidationException")
class ValidationExceptionTest {

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("should create with list of errors")
        void createsWithErrors() {
            List<ValidationError> errors = List.of(
                    new ValidationError("email", "required"),
                    new ValidationError("name", "too short")
            );

            ValidationException ex = new ValidationException(errors);

            assertThat(ex.errors()).hasSize(2);
            assertThat(ex.errorCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should create with single error")
        void createsWithSingleError() {
            ValidationError error = new ValidationError("email", "required");

            ValidationException ex = new ValidationException(error);

            assertThat(ex.errors()).hasSize(1);
            assertThat(ex.errors().getFirst()).isEqualTo(error);
        }

        @Test
        @DisplayName("should create with path and message")
        void createsWithPathAndMessage() {
            ValidationException ex = new ValidationException("email", "must be valid");

            assertThat(ex.errors()).hasSize(1);
            assertThat(ex.errors().getFirst().path()).isEqualTo("email");
            assertThat(ex.errors().getFirst().message()).isEqualTo("must be valid");
        }

        @Test
        @DisplayName("should throw on empty errors list")
        void throwsOnEmptyList() {
            assertThatThrownBy(() -> new ValidationException(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw on null errors")
        void throwsOnNull() {
            assertThatThrownBy(() -> new ValidationException((List<ValidationError>) null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Message")
    class MessageTests {

        @Test
        @DisplayName("should build message for single error")
        void singleErrorMessage() {
            ValidationException ex = new ValidationException("email", "required");

            assertThat(ex.getMessage()).contains("email");
            assertThat(ex.getMessage()).contains("required");
        }

        @Test
        @DisplayName("should build message for multiple errors")
        void multipleErrorsMessage() {
            List<ValidationError> errors = List.of(
                    new ValidationError("email", "required"),
                    new ValidationError("name", "too short")
            );

            ValidationException ex = new ValidationException(errors);

            assertThat(ex.getMessage()).contains("2 errors");
            assertThat(ex.getMessage()).contains("email");
            assertThat(ex.getMessage()).contains("name");
        }
    }

    @Nested
    @DisplayName("toErrorMap()")
    class ToErrorMapTests {

        @Test
        @DisplayName("should convert to map")
        void convertsToMap() {
            List<ValidationError> errors = List.of(
                    new ValidationError("email", "required"),
                    new ValidationError("email", "invalid format"),
                    new ValidationError("name", "too short")
            );

            ValidationException ex = new ValidationException(errors);
            Map<String, List<String>> map = ex.toErrorMap();

            assertThat(map).containsKey("email");
            assertThat(map).containsKey("name");
            assertThat(map.get("email")).containsExactly("required", "invalid format");
            assertThat(map.get("name")).containsExactly("too short");
        }
    }

    @Nested
    @DisplayName("errors immutability")
    class ImmutabilityTests {

        @Test
        @DisplayName("errors list should be immutable")
        void errorsAreImmutable() {
            ValidationException ex = new ValidationException("email", "required");

            assertThatThrownBy(() -> ex.errors().add(new ValidationError("x", "y")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
