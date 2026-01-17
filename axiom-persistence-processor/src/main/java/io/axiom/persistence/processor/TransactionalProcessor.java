package io.axiom.persistence.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Annotation processor for @Transactional annotation.
 *
 * <p>
 * Generates wrapper classes at compile time that wrap transactional methods
 * with the Transaction API. This approach:
 * <ul>
 *   <li>No runtime proxies or reflection</li>
 *   <li>Generated code is debuggable</li>
 *   <li>Type-safe at compile time</li>
 *   <li>Zero runtime overhead</li>
 * </ul>
 *
 * <h2>Generated Code Pattern</h2>
 * <p>
 * For a class like:
 * <pre>{@code
 * public class OrderService {
 *     @Transactional
 *     public void saveOrder(Order order) {
 *         repository.save(order);
 *     }
 * }
 * }</pre>
 *
 * <p>
 * Generates:
 * <pre>{@code
 * public class OrderService$Tx extends OrderService {
 *     private final DataSource dataSource;
 *
 *     public OrderService$Tx(DataSource dataSource) {
 *         this.dataSource = dataSource;
 *     }
 *
 *     @Override
 *     public void saveOrder(Order order) {
 *         Transaction.execute(dataSource, () -> super.saveOrder(order));
 *     }
 * }
 * }</pre>
 *
 * @since 0.1.0
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("io.axiom.persistence.tx.Transactional")
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class TransactionalProcessor extends AbstractProcessor {

    private static final String TRANSACTIONAL_ANNOTATION = "io.axiom.persistence.tx.Transactional";
    private static final String WRAPPER_SUFFIX = "$Tx";

    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        TypeElement transactionalAnnotation = elementUtils.getTypeElement(TRANSACTIONAL_ANNOTATION);
        if (transactionalAnnotation == null) {
            return false;
        }

        // Collect all classes that need wrapper generation
        Map<TypeElement, List<ExecutableElement>> classesWithTransactionalMethods = new LinkedHashMap<>();

        for (Element element : roundEnv.getElementsAnnotatedWith(transactionalAnnotation)) {
            if (element.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) element;
                TypeElement enclosingClass = (TypeElement) method.getEnclosingElement();

                if (!validateMethod(method)) {
                    continue;
                }

                classesWithTransactionalMethods
                        .computeIfAbsent(enclosingClass, k -> new ArrayList<>())
                        .add(method);
            } else if (element.getKind() == ElementKind.CLASS) {
                // Class-level @Transactional - all public methods become transactional
                TypeElement classElement = (TypeElement) element;
                List<ExecutableElement> methods = classElement.getEnclosedElements().stream()
                        .filter(e -> e.getKind() == ElementKind.METHOD)
                        .map(e -> (ExecutableElement) e)
                        .filter(this::isOverridable)
                        .filter(this::validateMethod)
                        .collect(Collectors.toList());

                if (!methods.isEmpty()) {
                    classesWithTransactionalMethods.put(classElement, methods);
                }
            }
        }

        // Generate wrapper for each class
        for (Map.Entry<TypeElement, List<ExecutableElement>> entry : classesWithTransactionalMethods.entrySet()) {
            try {
                generateWrapper(entry.getKey(), entry.getValue());
            } catch (IOException e) {
                error(entry.getKey(), "Failed to generate transactional wrapper: %s", e.getMessage());
            }
        }

        return true;
    }

    private boolean validateMethod(ExecutableElement method) {
        // Must be overridable (not private, not static, not final)
        if (!isOverridable(method)) {
            error(method, "@Transactional methods must be overridable (not private, static, or final)");
            return false;
        }

        // Enclosing class must not be final
        TypeElement enclosingClass = (TypeElement) method.getEnclosingElement();
        if (enclosingClass.getModifiers().contains(Modifier.FINAL)) {
            error(method, "@Transactional methods cannot be in final classes");
            return false;
        }

        return true;
    }

    private boolean isOverridable(ExecutableElement method) {
        Set<Modifier> modifiers = method.getModifiers();
        return !modifiers.contains(Modifier.PRIVATE)
                && !modifiers.contains(Modifier.STATIC)
                && !modifiers.contains(Modifier.FINAL);
    }

    private void generateWrapper(TypeElement originalClass, List<ExecutableElement> transactionalMethods)
            throws IOException {
        String packageName = elementUtils.getPackageOf(originalClass).getQualifiedName().toString();
        String originalClassName = originalClass.getSimpleName().toString();
        String wrapperClassName = originalClassName + WRAPPER_SUFFIX;

        ClassName dataSourceType = ClassName.get("javax.sql", "DataSource");
        ClassName transactionType = ClassName.get("io.axiom.persistence.tx", "Transaction");
        ClassName isolationLevelType = ClassName.get("io.axiom.persistence.tx", "IsolationLevel");
        ClassName propagationType = ClassName.get("io.axiom.persistence.tx", "Propagation");

        // Build the wrapper class
        TypeSpec.Builder wrapperBuilder = TypeSpec.classBuilder(wrapperClassName)
                .addModifiers(Modifier.PUBLIC)
                .superclass(TypeName.get(originalClass.asType()))
                .addJavadoc("Generated transactional wrapper for {@link $T}.\n", originalClass)
                .addJavadoc("\n<p>This class is auto-generated by the Axiom annotation processor.\n")
                .addJavadoc("Do not modify manually.\n")
                .addJavadoc("\n@since 0.1.0\n");

        // Add DataSource field
        wrapperBuilder.addField(FieldSpec.builder(dataSourceType, "dataSource", Modifier.PRIVATE, Modifier.FINAL)
                .build());

        // Generate constructors that mirror the original class constructors
        List<ExecutableElement> constructors = originalClass.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
                .map(e -> (ExecutableElement) e)
                .filter(c -> !c.getModifiers().contains(Modifier.PRIVATE))
                .collect(Collectors.toList());

        if (constructors.isEmpty()) {
            // Default constructor
            wrapperBuilder.addMethod(MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(dataSourceType, "dataSource")
                    .addStatement("this.dataSource = $T.requireNonNull(dataSource, $S)",
                            Objects.class, "dataSource")
                    .build());
        } else {
            for (ExecutableElement constructor : constructors) {
                MethodSpec.Builder ctorBuilder = MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(dataSourceType, "dataSource");

                // Add original constructor parameters
                List<String> superParams = new ArrayList<>();
                for (VariableElement param : constructor.getParameters()) {
                    String paramName = param.getSimpleName().toString();
                    ctorBuilder.addParameter(TypeName.get(param.asType()), paramName);
                    superParams.add(paramName);
                }

                // Call super constructor
                if (superParams.isEmpty()) {
                    ctorBuilder.addStatement("super()");
                } else {
                    ctorBuilder.addStatement("super($L)", String.join(", ", superParams));
                }
                ctorBuilder.addStatement("this.dataSource = $T.requireNonNull(dataSource, $S)",
                        Objects.class, "dataSource");

                wrapperBuilder.addMethod(ctorBuilder.build());
            }
        }

        // Generate overriding methods for each transactional method
        for (ExecutableElement method : transactionalMethods) {
            wrapperBuilder.addMethod(generateTransactionalMethod(method, transactionType,
                    isolationLevelType, propagationType));
        }

        // Write the generated class
        JavaFile javaFile = JavaFile.builder(packageName, wrapperBuilder.build())
                .addFileComment("Generated by Axiom @Transactional Processor - DO NOT EDIT")
                .indent("    ")
                .build();

        javaFile.writeTo(filer);

        note(originalClass, "Generated transactional wrapper: %s.%s", packageName, wrapperClassName);
    }

    private MethodSpec generateTransactionalMethod(ExecutableElement method,
                                                   ClassName transactionType,
                                                   ClassName isolationLevelType,
                                                   ClassName propagationType) {
        String methodName = method.getSimpleName().toString();
        TypeMirror returnType = method.getReturnType();
        boolean isVoid = returnType.getKind() == TypeKind.VOID;

        // Get annotation values
        AnnotationMirror transactionalAnnotation = getAnnotationMirror(method, TRANSACTIONAL_ANNOTATION);
        TransactionalConfig config = extractConfig(transactionalAnnotation);

        // Build the method signature
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addAnnotation(Override.class)
                .addModifiers(getVisibleModifiers(method))
                .returns(TypeName.get(returnType));

        // Add parameters
        List<String> paramNames = new ArrayList<>();
        for (VariableElement param : method.getParameters()) {
            String paramName = param.getSimpleName().toString();
            paramNames.add(paramName);
            methodBuilder.addParameter(TypeName.get(param.asType()), paramName);
        }

        // Add thrown exceptions
        for (TypeMirror thrownType : method.getThrownTypes()) {
            methodBuilder.addException(TypeName.get(thrownType));
        }

        // Build the transaction call
        String superCall = paramNames.isEmpty()
                ? "super." + methodName + "()"
                : "super." + methodName + "(" + String.join(", ", paramNames) + ")";

        // Generate Transaction.builder() chain
        CodeBlock.Builder txBuilder = CodeBlock.builder()
                .add("$T.builder(dataSource)", transactionType);

        // Add isolation level if not default
        if (!"DEFAULT".equals(config.isolation)) {
            txBuilder.add("\n    .isolation($T.$L)", isolationLevelType, config.isolation);
        }

        // Add propagation if not REQUIRED
        if (!"REQUIRED".equals(config.propagation)) {
            txBuilder.add("\n    .propagation($T.$L)", propagationType, config.propagation);
        }

        // Add readOnly if true
        if (config.readOnly) {
            txBuilder.add("\n    .readOnly(true)");
        }

        // Add timeout if specified
        if (config.timeout > 0) {
            txBuilder.add("\n    .timeout($L)", config.timeout);
        }

        // Add label if specified
        if (config.label != null && !config.label.isEmpty()) {
            txBuilder.add("\n    .name($S)", config.label);
        }

        // Add rollbackFor exceptions
        for (String exceptionType : config.rollbackFor) {
            txBuilder.add("\n    .rollbackFor($L.class)", exceptionType);
        }

        // Add noRollbackFor exceptions
        for (String exceptionType : config.noRollbackFor) {
            txBuilder.add("\n    .noRollbackFor($L.class)", exceptionType);
        }

        // Execute the transaction
        if (isVoid) {
            methodBuilder.addStatement(CodeBlock.builder()
                    .add(txBuilder.build())
                    .add("\n    .execute(() -> { $L; })", superCall)
                    .build());
        } else {
            methodBuilder.addStatement(CodeBlock.builder()
                    .add("return ")
                    .add(txBuilder.build())
                    .add("\n    .execute(() -> $L)", superCall)
                    .build());
        }

        return methodBuilder.build();
    }

    private Set<Modifier> getVisibleModifiers(ExecutableElement method) {
        Set<Modifier> modifiers = new LinkedHashSet<>();
        for (Modifier mod : method.getModifiers()) {
            if (mod == Modifier.PUBLIC || mod == Modifier.PROTECTED) {
                modifiers.add(mod);
            }
        }
        if (modifiers.isEmpty()) {
            // Package-private - no modifier needed
        }
        return modifiers;
    }

    private AnnotationMirror getAnnotationMirror(Element element, String annotationClassName) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().toString().equals(annotationClassName)) {
                return mirror;
            }
        }
        // Check if annotation is on the enclosing class
        Element enclosing = element.getEnclosingElement();
        if (enclosing != null && enclosing.getKind() == ElementKind.CLASS) {
            for (AnnotationMirror mirror : enclosing.getAnnotationMirrors()) {
                if (mirror.getAnnotationType().toString().equals(annotationClassName)) {
                    return mirror;
                }
            }
        }
        return null;
    }

    private TransactionalConfig extractConfig(AnnotationMirror annotation) {
        TransactionalConfig config = new TransactionalConfig();

        if (annotation == null) {
            return config;
        }

        Map<? extends ExecutableElement, ? extends AnnotationValue> values =
                elementUtils.getElementValuesWithDefaults(annotation);

        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
            String name = entry.getKey().getSimpleName().toString();
            Object value = entry.getValue().getValue();

            switch (name) {
                case "isolation" -> config.isolation = extractEnumName(value);
                case "propagation" -> config.propagation = extractEnumName(value);
                case "readOnly" -> config.readOnly = (Boolean) value;
                case "timeout" -> config.timeout = (Integer) value;
                case "label" -> config.label = (String) value;
                case "rollbackFor" -> config.rollbackFor = extractClassNames(value);
                case "rollbackForClassName" -> config.rollbackFor.addAll(extractStringList(value));
                case "noRollbackFor" -> config.noRollbackFor = extractClassNames(value);
                case "noRollbackForClassName" -> config.noRollbackFor.addAll(extractStringList(value));
            }
        }

        return config;
    }

    private String extractEnumName(Object value) {
        if (value instanceof VariableElement) {
            return ((VariableElement) value).getSimpleName().toString();
        }
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractClassNames(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (item instanceof AnnotationValue) {
                    Object itemValue = ((AnnotationValue) item).getValue();
                    if (itemValue instanceof DeclaredType) {
                        result.add(((DeclaredType) itemValue).asElement().toString());
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (item instanceof AnnotationValue) {
                    result.add(((AnnotationValue) item).getValue().toString());
                }
            }
        }
        return result;
    }

    private void error(Element element, String message, Object... args) {
        messager.printMessage(Diagnostic.Kind.ERROR, String.format(message, args), element);
    }

    private void note(Element element, String message, Object... args) {
        messager.printMessage(Diagnostic.Kind.NOTE, String.format(message, args), element);
    }

    /**
     * Holds extracted @Transactional configuration values.
     */
    private static class TransactionalConfig {
        String isolation = "DEFAULT";
        String propagation = "REQUIRED";
        boolean readOnly = false;
        int timeout = -1;
        String label = "";
        List<String> rollbackFor = new ArrayList<>();
        List<String> noRollbackFor = new ArrayList<>();
    }
}
