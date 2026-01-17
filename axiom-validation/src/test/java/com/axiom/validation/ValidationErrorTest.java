package com.axiom.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ValidationError record.
 */
@DisplayName("ValidationError")
class ValidationErrorTest {

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("should create with all fields")
        void createsWithAllFields() {
            ValidationError error = new ValidationError("email", "required", "bad-value");

            assertThat(error.path()).isEqualTo("email");
            assertThat(error.message()).isEqualTo("required");
            assertThat(error.invalidValue()).isEqualTo("bad-value");
        }

        @Test
        @DisplayName("should create without invalid value")
        void createsWithoutInvalidValue() {
            ValidationError error = new ValidationError("email", "required");

            assertThat(error.path()).isEqualTo("email");
            assertThat(error.message()).isEqualTo("required");
            assertThat(error.invalidValue()).isNull();
        }

        @Test
        @DisplayName("should allow null invalid value")
        void allowsNullInvalidValue() {
            ValidationError error = new ValidationError("email", "required", null);

            assertThat(error.invalidValue()).isNull();
        }

        @Test
        @DisplayName("should throw on null path")
        void throwsOnNullPath() {
            assertThatThrownBy(() -> new ValidationError(null, "message"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should throw on null message")
        void throwsOnNullMessage() {
            assertThatThrownBy(() -> new ValidationError("path", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("should format without invalid value")
        void formatsWithoutValue() {
            ValidationError error = new ValidationError("email", "must not be blank");

            assertThat(error.toString()).isEqualTo("email: must not be blank");
        }

        @Test
        @DisplayName("should format with invalid value")
        void formatsWithValue() {
            ValidationError error = new ValidationError("age", "must be at least 18", 10);

            assertThat(error.toString()).isEqualTo("age: must be at least 18 (was: 10)");
        }

        @Test
        @DisplayName("should format with null invalid value")
        void formatsWithNullValue() {
            ValidationError error = new ValidationError("name", "required", null);

            assertThat(error.toString()).isEqualTo("name: required");
        }
    }

    @Nested
    @DisplayName("Equality")
    class EqualityTests {

        @Test
        @DisplayName("should be equal for same values")
        void equalForSameValues() {
            ValidationError error1 = new ValidationError("email", "required", "bad");
            ValidationError error2 = new ValidationError("email", "required", "bad");

            assertThat(error1).isEqualTo(error2);
            assertThat(error1.hashCode()).isEqualTo(error2.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different paths")
        void notEqualForDifferentPaths() {
            ValidationError error1 = new ValidationError("email", "required");
            ValidationError error2 = new ValidationError("name", "required");

            assertThat(error1).isNotEqualTo(error2);
        }
    }
}
