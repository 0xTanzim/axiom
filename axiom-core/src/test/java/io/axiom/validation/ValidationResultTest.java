package io.axiom.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ValidationResult sealed interface.
 */
@DisplayName("ValidationResult")
class ValidationResultTest {

    @Nested
    @DisplayName("Valid")
    class ValidTests {

        @Test
        @DisplayName("should be valid")
        void isValid() {
            ValidationResult.Valid<String> result = new ValidationResult.Valid<>("test");

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("should return value")
        void returnsValue() {
            ValidationResult.Valid<String> result = new ValidationResult.Valid<>("test");

            assertThat(result.value()).isEqualTo("test");
        }

        @Test
        @DisplayName("should have empty errors")
        void hasEmptyErrors() {
            ValidationResult.Valid<String> result = new ValidationResult.Valid<>("test");

            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("getOrThrow should return value")
        void getOrThrowReturnsValue() {
            ValidationResult.Valid<String> result = new ValidationResult.Valid<>("test");

            assertThat(result.getOrThrow()).isEqualTo("test");
        }

        @Test
        @DisplayName("should throw on null value")
        void throwsOnNullValue() {
            assertThatThrownBy(() -> new ValidationResult.Valid<>(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Invalid")
    class InvalidTests {

        @Test
        @DisplayName("should not be valid")
        void isNotValid() {
            List<ValidationError> errors = List.of(new ValidationError("field", "error"));
            ValidationResult.Invalid<String> result = new ValidationResult.Invalid<>("test", errors);

            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("should return value")
        void returnsValue() {
            List<ValidationError> errors = List.of(new ValidationError("field", "error"));
            ValidationResult.Invalid<String> result = new ValidationResult.Invalid<>("test", errors);

            assertThat(result.value()).isEqualTo("test");
        }

        @Test
        @DisplayName("should return errors")
        void returnsErrors() {
            List<ValidationError> errors = List.of(
                    new ValidationError("field1", "error1"),
                    new ValidationError("field2", "error2")
            );
            ValidationResult.Invalid<String> result = new ValidationResult.Invalid<>("test", errors);

            assertThat(result.errors()).hasSize(2);
        }

        @Test
        @DisplayName("errors should be immutable")
        void errorsAreImmutable() {
            List<ValidationError> errors = List.of(new ValidationError("field", "error"));
            ValidationResult.Invalid<String> result = new ValidationResult.Invalid<>("test", errors);

            assertThatThrownBy(() -> result.errors().add(new ValidationError("x", "y")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("getOrThrow should throw ValidationException")
        void getOrThrowThrows() {
            List<ValidationError> errors = List.of(new ValidationError("field", "error"));
            ValidationResult.Invalid<String> result = new ValidationResult.Invalid<>("test", errors);

            assertThatThrownBy(result::getOrThrow)
                    .isInstanceOf(ValidationException.class)
                    .satisfies(e -> {
                        ValidationException ve = (ValidationException) e;
                        assertThat(ve.errors()).hasSize(1);
                    });
        }

        @Test
        @DisplayName("should throw on null value")
        void throwsOnNullValue() {
            List<ValidationError> errors = List.of(new ValidationError("field", "error"));

            assertThatThrownBy(() -> new ValidationResult.Invalid<>(null, errors))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should throw on null errors")
        void throwsOnNullErrors() {
            assertThatThrownBy(() -> new ValidationResult.Invalid<>("test", null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should throw on empty errors")
        void throwsOnEmptyErrors() {
            assertThatThrownBy(() -> new ValidationResult.Invalid<>("test", List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Pattern matching")
    class PatternMatchingTests {

        @Test
        @DisplayName("should support switch pattern matching")
        void supportsSwitchPatternMatching() {
            ValidationResult<String> result = new ValidationResult.Valid<>("test");

            String outcome = switch (result) {
                case ValidationResult.Valid<String> v -> "valid: " + v.value();
                case ValidationResult.Invalid<String> i -> "invalid: " + i.errors().size();
            };

            assertThat(outcome).isEqualTo("valid: test");
        }
    }
}
