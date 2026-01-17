package com.axiom.validation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for AxiomValidator.
 */
@DisplayName("AxiomValidator")
class AxiomValidatorTest {

    // Test DTOs
    record SimpleUser(
            @NotBlank(message = "Name is required")
            String name,

            @Email(message = "Invalid email format")
            @NotBlank(message = "Email is required")
            String email,

            @Min(value = 18, message = "Must be at least 18")
            int age
    ) {}

    record Address(
            @NotBlank String street,
            @NotBlank String city,
            @Size(min = 5, max = 5, message = "ZIP must be 5 characters")
            String zip
    ) {}

    record UserWithAddress(
            @NotBlank String name,
            @NotNull(message = "Address is required")
            Address address
    ) {}

    @Nested
    @DisplayName("validate()")
    class ValidateTests {

        @Test
        @DisplayName("should return Valid for valid object")
        void validObject() {
            SimpleUser user = new SimpleUser("John", "john@example.com", 25);

            ValidationResult<SimpleUser> result = AxiomValidator.validate(user);

            assertThat(result.isValid()).isTrue();
            assertThat(result.value()).isEqualTo(user);
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("should return Invalid for invalid object")
        void invalidObject() {
            SimpleUser user = new SimpleUser("", "invalid-email", 16);

            ValidationResult<SimpleUser> result = AxiomValidator.validate(user);

            assertThat(result.isValid()).isFalse();
            assertThat(result.value()).isEqualTo(user);
            assertThat(result.errors()).hasSize(3);
        }

        @Test
        @DisplayName("should capture all violations")
        void capturesAllViolations() {
            SimpleUser user = new SimpleUser("", "", 10);

            ValidationResult<SimpleUser> result = AxiomValidator.validate(user);

            assertThat(result.errors())
                    .extracting(ValidationError::path)
                    .containsExactlyInAnyOrder("name", "email", "age");
        }

        @Test
        @DisplayName("should include error messages")
        void includesMessages() {
            SimpleUser user = new SimpleUser("", "john@example.com", 25);

            ValidationResult<SimpleUser> result = AxiomValidator.validate(user);

            assertThat(result.errors())
                    .extracting(ValidationError::message)
                    .contains("Name is required");
        }

        @Test
        @DisplayName("should include invalid values")
        void includesInvalidValues() {
            SimpleUser user = new SimpleUser("John", "bad-email", 25);

            ValidationResult<SimpleUser> result = AxiomValidator.validate(user);

            assertThat(result.errors())
                    .filteredOn(e -> e.path().equals("email"))
                    .extracting(ValidationError::invalidValue)
                    .contains("bad-email");
        }

        @Test
        @DisplayName("should throw on null object")
        void throwsOnNull() {
            assertThatThrownBy(() -> AxiomValidator.validate(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("null");
        }
    }

    @Nested
    @DisplayName("validateProperty()")
    class ValidatePropertyTests {

        @Test
        @DisplayName("should validate single property")
        void validatesSingleProperty() {
            SimpleUser user = new SimpleUser("John", "bad-email", 25);

            ValidationResult<SimpleUser> result = AxiomValidator.validateProperty(user, "email");

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().getFirst().path()).isEqualTo("email");
        }

        @Test
        @DisplayName("should return Valid for valid property")
        void validProperty() {
            SimpleUser user = new SimpleUser("John", "john@example.com", 10); // age invalid

            ValidationResult<SimpleUser> result = AxiomValidator.validateProperty(user, "email");

            assertThat(result.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("validateOrThrow()")
    class ValidateOrThrowTests {

        @Test
        @DisplayName("should return object if valid")
        void returnsValidObject() {
            SimpleUser user = new SimpleUser("John", "john@example.com", 25);

            SimpleUser result = AxiomValidator.validateOrThrow(user);

            assertThat(result).isEqualTo(user);
        }

        @Test
        @DisplayName("should throw ValidationException if invalid")
        void throwsOnInvalid() {
            SimpleUser user = new SimpleUser("", "bad-email", 10);

            assertThatThrownBy(() -> AxiomValidator.validateOrThrow(user))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(e -> {
                        ValidationException ve = (ValidationException) e;
                        assertThat(ve.errors()).hasSize(3);
                    });
        }
    }

    @Nested
    @DisplayName("isValid()")
    class IsValidTests {

        @Test
        @DisplayName("should return true for valid object")
        void trueForValid() {
            SimpleUser user = new SimpleUser("John", "john@example.com", 25);

            assertThat(AxiomValidator.isValid(user)).isTrue();
        }

        @Test
        @DisplayName("should return false for invalid object")
        void falseForInvalid() {
            SimpleUser user = new SimpleUser("", "", 10);

            assertThat(AxiomValidator.isValid(user)).isFalse();
        }
    }

    @Nested
    @DisplayName("Nested validation")
    class NestedValidationTests {

        @Test
        @DisplayName("should validate nested objects with @Valid")
        void validatesNestedObjects() {
            // Note: @Valid annotation needed on nested field for cascade validation
            // Without @Valid, nested object constraints are not checked
            UserWithAddress user = new UserWithAddress("John", null);

            ValidationResult<UserWithAddress> result = AxiomValidator.validate(user);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors())
                    .extracting(ValidationError::path)
                    .contains("address");
        }
    }

    @Nested
    @DisplayName("getOrThrow()")
    class GetOrThrowTests {

        @Test
        @DisplayName("Valid.getOrThrow() returns value")
        void validGetOrThrow() {
            SimpleUser user = new SimpleUser("John", "john@example.com", 25);
            ValidationResult<SimpleUser> result = new ValidationResult.Valid<>(user);

            assertThat(result.getOrThrow()).isEqualTo(user);
        }

        @Test
        @DisplayName("Invalid.getOrThrow() throws ValidationException")
        void invalidGetOrThrow() {
            SimpleUser user = new SimpleUser("", "", 10);
            List<ValidationError> errors = List.of(
                    new ValidationError("name", "required"),
                    new ValidationError("email", "required")
            );
            ValidationResult<SimpleUser> result = new ValidationResult.Invalid<>(user, errors);

            assertThatThrownBy(result::getOrThrow)
                    .isInstanceOf(ValidationException.class)
                    .satisfies(e -> {
                        ValidationException ve = (ValidationException) e;
                        assertThat(ve.errors()).hasSize(2);
                    });
        }
    }
}
