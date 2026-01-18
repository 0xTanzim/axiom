package io.axiom.persistence.tx;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;

/**
 * Tests for TransactionalValidator.
 */
@DisplayName("TransactionalValidator")
class TransactionalValidatorTest {

    @Nested
    @DisplayName("validate()")
    class ValidateTests {

        @Test
        @DisplayName("passes for class without @Transactional")
        void passesForClassWithoutTransactional() {
            // Should not throw
            TransactionalValidator.validate(PlainClass.class);
        }

        @Test
        @DisplayName("throws for class with @Transactional but no $Tx wrapper")
        void throwsForMissingWrapper() {
            Assertions.assertThatThrownBy(() -> TransactionalValidator.validate(UnprocessedService.class))
                    .isInstanceOf(TransactionalValidator.TransactionalValidationException.class)
                    .hasMessageContaining("UnprocessedService")
                    .hasMessageContaining("axiom-persistence-processor");
        }

        @Test
        @DisplayName("throws for class with class-level @Transactional but no $Tx wrapper")
        void throwsForClassLevelTransactional() {
            Assertions.assertThatThrownBy(() -> TransactionalValidator.validate(UnprocessedClassLevel.class))
                    .isInstanceOf(TransactionalValidator.TransactionalValidationException.class)
                    .hasMessageContaining("UnprocessedClassLevel");
        }

        @Test
        @DisplayName("collects multiple errors")
        void collectsMultipleErrors() {
            final var exception = Assertions.catchThrowableOfType(
                    () -> TransactionalValidator.validate(UnprocessedService.class, UnprocessedClassLevel.class),
                    TransactionalValidator.TransactionalValidationException.class
            );

            Assertions.assertThat(exception.errors()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("validateClass()")
    class ValidateClassTests {

        @Test
        @DisplayName("returns null for valid class")
        void returnsNullForValid() {
            final var error = TransactionalValidator.validateClass(PlainClass.class);
            Assertions.assertThat(error).isNull();
        }

        @Test
        @DisplayName("returns error for invalid class")
        void returnsErrorForInvalid() {
            final var error = TransactionalValidator.validateClass(UnprocessedService.class);

            Assertions.assertThat(error).isNotNull();
            Assertions.assertThat(error.originalClass()).isEqualTo(UnprocessedService.class);
            Assertions.assertThat(error.expectedWrapper()).contains("UnprocessedService$Tx");
        }
    }

    @Nested
    @DisplayName("checkWithWarning()")
    class CheckWithWarningTests {

        @Test
        @DisplayName("returns true for valid classes")
        void returnsTrueForValid() {
            final boolean result = TransactionalValidator.checkWithWarning(PlainClass.class);
            Assertions.assertThat(result).isTrue();
        }

        @Test
        @DisplayName("returns false for invalid classes (does not throw)")
        void returnsFalseForInvalid() {
            final boolean result = TransactionalValidator.checkWithWarning(UnprocessedService.class);
            Assertions.assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("ValidationError")
    class ValidationErrorTests {

        @Test
        @DisplayName("message contains class name")
        void messageContainsClassName() {
            final var error = new TransactionalValidator.ValidationError(
                    UnprocessedService.class,
                    "io.axiom.persistence.tx.UnprocessedService$Tx"
            );

            Assertions.assertThat(error.message())
                    .contains("UnprocessedService")
                    .contains("@Transactional");
        }

        @Test
        @DisplayName("detailed message contains fix instructions")
        void detailedMessageContainsFix() {
            final var error = new TransactionalValidator.ValidationError(
                    UnprocessedService.class,
                    "io.axiom.persistence.tx.UnprocessedService$Tx"
            );

            final String detailed = error.toDetailedMessage();

            Assertions.assertThat(detailed)
                    .contains("MAVEN")
                    .contains("GRADLE")
                    .contains("axiom-persistence-processor")
                    .contains("annotationProcessor")
                    .contains("UnprocessedService$Tx");
        }
    }

    // Test fixtures

    /**
     * Plain class without @Transactional - should pass validation.
     */
    static class PlainClass {
        public void doSomething() {
        }
    }

    /**
     * Class with @Transactional method but no processor - should fail validation.
     */
    static class UnprocessedService {
        @Transactional
        public void save() {
        }
    }

    /**
     * Class with class-level @Transactional but no processor - should fail validation.
     */
    @Transactional
    static class UnprocessedClassLevel {
        public void save() {
        }
    }
}
