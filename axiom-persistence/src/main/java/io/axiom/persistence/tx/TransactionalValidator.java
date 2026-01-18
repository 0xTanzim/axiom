package io.axiom.persistence.tx;

import java.lang.reflect.*;
import java.util.*;

import org.slf4j.*;

/**
 * Runtime validator for @Transactional annotation usage.
 *
 * <p>
 * Validates that classes with @Transactional methods have been processed
 * by the annotation processor. If the processor is not configured,
 * @Transactional annotations silently do nothing, which leads to
 * unexpected behavior.
 *
 * <h2>Usage</h2>
 * <p>
 * Call at application startup to detect missing processor configuration:
 * <pre>{@code
 * // Check specific classes
 * TransactionalValidator.validate(UserRepository.class, OrderService.class);
 *
 * // Or scan a package (requires classpath scanning)
 * TransactionalValidator.validateAll(UserRepository.class, OrderService.class);
 * }</pre>
 *
 * <p>
 * If @Transactional methods exist but no $Tx wrapper was generated:
 * <pre>
 * ERROR: Class 'UserRepository' has @Transactional methods but no generated
 *        wrapper class 'UserRepository$Tx' was found.
 *
 *        FIX: Add axiom-persistence-processor to your build:
 *
 *        Maven:
 *        &lt;annotationProcessorPaths&gt;
 *            &lt;path&gt;
 *                &lt;groupId&gt;io.axiom&lt;/groupId&gt;
 *                &lt;artifactId&gt;axiom-persistence-processor&lt;/artifactId&gt;
 *                &lt;version&gt;${axiom.version}&lt;/version&gt;
 *            &lt;/path&gt;
 *        &lt;/annotationProcessorPaths&gt;
 *
 *        Gradle:
 *        annotationProcessor 'io.axiom:axiom-persistence-processor:${axiomVersion}'
 * </pre>
 *
 * @since 0.1.0
 */
public final class TransactionalValidator {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionalValidator.class);
    private static final String TX_SUFFIX = "$Tx";

    private TransactionalValidator() {
    }

    /**
     * Validates that classes with @Transactional methods have generated wrappers.
     *
     * <p>
     * Call this at application startup to fail fast if the annotation
     * processor is not configured.
     *
     * @param classes the classes to validate
     * @throws TransactionalValidationException if validation fails
     */
    public static void validate(final Class<?>... classes) {
        final List<ValidationError> errors = new ArrayList<>();

        for (final Class<?> clazz : classes) {
            final ValidationError error = TransactionalValidator.validateClass(clazz);
            if (error != null) {
                errors.add(error);
            }
        }

        if (!errors.isEmpty()) {
            throw new TransactionalValidationException(errors);
        }
    }

    /**
     * Validates a single class and returns error if found.
     *
     * @param clazz the class to validate
     * @return validation error, or null if valid
     */
    public static ValidationError validateClass(final Class<?> clazz) {
        if (!TransactionalValidator.hasTransactionalMethods(clazz)) {
            return null;
        }

        final String wrapperClassName = clazz.getName() + TransactionalValidator.TX_SUFFIX;

        try {
            Class.forName(wrapperClassName);
            TransactionalValidator.LOG.debug("Validated: {} has generated wrapper {}", clazz.getSimpleName(), wrapperClassName);
            return null;
        } catch (final ClassNotFoundException e) {
            return new ValidationError(clazz, wrapperClassName);
        }
    }

    /**
     * Checks if a class has @Transactional methods but warns instead of throwing.
     *
     * <p>
     * Use this for soft validation during development.
     *
     * @param classes the classes to check
     * @return true if all classes are valid
     */
    public static boolean checkWithWarning(final Class<?>... classes) {
        boolean allValid = true;

        for (final Class<?> clazz : classes) {
            final ValidationError error = TransactionalValidator.validateClass(clazz);
            if (error != null) {
                TransactionalValidator.LOG.warn(error.toDetailedMessage());
                allValid = false;
            }
        }

        return allValid;
    }

    private static boolean hasTransactionalMethods(final Class<?> clazz) {
        // Check class-level annotation
        if (clazz.isAnnotationPresent(Transactional.class)) {
            return true;
        }

        // Check method-level annotations
        for (final Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Transactional.class)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Represents a validation error for a class missing its $Tx wrapper.
     */
    public record ValidationError(Class<?> originalClass, String expectedWrapper) {

        /**
         * Returns a brief error message.
         *
         * @return the error message
         */
        public String message() {
            return "Class '" + this.originalClass.getSimpleName() + "' has @Transactional methods " +
                    "but no generated wrapper '" + this.expectedWrapper + "' was found.";
        }

        /**
         * Returns a detailed error message with fix instructions.
         *
         * @return the detailed message with fix
         */
        public String toDetailedMessage() {
            return """

                    ══════════════════════════════════════════════════════════════════════════════
                    @Transactional PROCESSOR NOT CONFIGURED
                    ══════════════════════════════════════════════════════════════════════════════

                    Class: %s
                    Expected wrapper: %s

                    The @Transactional annotation requires the axiom-persistence-processor to
                    generate transaction wrapper classes at compile time. Without it, the
                    @Transactional annotation has NO EFFECT - methods will NOT run in transactions.

                    FIX - Add the processor to your build:

                    MAVEN (in pom.xml):
                    ┌─────────────────────────────────────────────────────────────────────────────
                    │ <plugin>
                    │   <groupId>org.apache.maven.plugins</groupId>
                    │   <artifactId>maven-compiler-plugin</artifactId>
                    │   <configuration>
                    │     <annotationProcessorPaths>
                    │       <path>
                    │         <groupId>io.axiom</groupId>
                    │         <artifactId>axiom-persistence-processor</artifactId>
                    │         <version>${axiom.version}</version>
                    │       </path>
                    │     </annotationProcessorPaths>
                    │   </configuration>
                    │ </plugin>
                    └─────────────────────────────────────────────────────────────────────────────

                    GRADLE (in build.gradle):
                    ┌─────────────────────────────────────────────────────────────────────────────
                    │ dependencies {
                    │     annotationProcessor 'io.axiom:axiom-persistence-processor:${axiomVersion}'
                    │ }
                    └─────────────────────────────────────────────────────────────────────────────

                    USAGE - After adding processor, use the generated $Tx class:
                    ┌─────────────────────────────────────────────────────────────────────────────
                    │ // Instead of:
                    │ // %s service = new %s();
                    │
                    │ // Use the generated wrapper:
                    │ %s$Tx service = new %s$Tx(dataSource);
                    │ service.yourMethod();  // Now wrapped in transaction!
                    └─────────────────────────────────────────────────────────────────────────────

                    ══════════════════════════════════════════════════════════════════════════════
                    """.formatted(
                    this.originalClass.getName(),
                    this.expectedWrapper,
                    this.originalClass.getSimpleName(),
                    this.originalClass.getSimpleName(),
                    this.originalClass.getSimpleName(),
                    this.originalClass.getSimpleName()
            );
        }
    }

    /**
     * Exception thrown when @Transactional validation fails.
     */
    public static final class TransactionalValidationException extends RuntimeException {

        private final List<ValidationError> errors;

        public TransactionalValidationException(final List<ValidationError> errors) {
            super(TransactionalValidationException.buildMessage(errors));
            this.errors = List.copyOf(errors);
        }

        /**
         * Returns the validation errors.
         *
         * @return list of validation errors
         */
        public List<ValidationError> errors() {
            return this.errors;
        }

        private static String buildMessage(final List<ValidationError> errors) {
            final StringBuilder sb = new StringBuilder();
            sb.append("@Transactional processor not configured for ").append(errors.size()).append(" class(es):\n\n");

            for (final ValidationError error : errors) {
                sb.append(error.toDetailedMessage());
            }

            return sb.toString();
        }
    }
}
