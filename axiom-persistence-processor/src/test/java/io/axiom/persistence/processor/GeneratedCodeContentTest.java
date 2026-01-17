package io.axiom.persistence.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that verify the generated source code content.
 */
class GeneratedCodeContentTest {

    @Test
    void generatedCodeContainsExpectedStructure() throws IOException {
        JavaFileObject source = JavaFileObjects.forSourceString("test.OrderService", """
            package test;

            import io.axiom.persistence.tx.Transactional;

            public class OrderService {
                @Transactional
                public void saveOrder(String orderId) {
                    // save logic
                }
            }
            """);

        Compilation compilation = javac()
                .withProcessors(new TransactionalProcessor())
                .withOptions("--enable-preview", "--release", "25")
                .compile(source);

        assertThat(compilation).succeeded();

        var generatedFile = compilation.generatedSourceFile("test.OrderService$Tx");
        assertThat(generatedFile).isPresent();

        String generatedCode = generatedFile.get().getCharContent(true).toString();

        // Verify structure
        assertThat(generatedCode)
                .contains("public class OrderService$Tx extends OrderService")
                .contains("private final DataSource dataSource")
                .contains("public OrderService$Tx(DataSource dataSource)")
                .contains("@Override")
                .contains("public void saveOrder(String orderId)")
                .contains("Transaction.builder(dataSource)")
                .contains("super.saveOrder(orderId)");
    }

    @Test
    void generatedCodeWithFullTransactionalOptions() throws IOException {
        JavaFileObject source = JavaFileObjects.forSourceString("test.PaymentService", """
            package test;

            import io.axiom.persistence.tx.Transactional;
            import io.axiom.persistence.tx.IsolationLevel;
            import io.axiom.persistence.tx.Propagation;

            public class PaymentService {
                @Transactional(
                    isolation = IsolationLevel.SERIALIZABLE,
                    propagation = Propagation.REQUIRES_NEW,
                    readOnly = true,
                    timeout = 30,
                    label = "process-payment"
                )
                public String processPayment(String paymentId, double amount) {
                    return "processed-" + paymentId;
                }
            }
            """);

        Compilation compilation = javac()
                .withProcessors(new TransactionalProcessor())
                .withOptions("--enable-preview", "--release", "25")
                .compile(source);

        assertThat(compilation).succeeded();

        var generatedFile = compilation.generatedSourceFile("test.PaymentService$Tx");
        assertThat(generatedFile).isPresent();

        String generatedCode = generatedFile.get().getCharContent(true).toString();

        // Verify all options are included
        assertThat(generatedCode)
                .contains(".isolation(IsolationLevel.SERIALIZABLE)")
                .contains(".propagation(Propagation.REQUIRES_NEW)")
                .contains(".readOnly(true)")
                .contains(".timeout(30)")
                .contains(".name(\"process-payment\")")
                .contains("return ")
                .contains("super.processPayment(paymentId, amount)");
    }

    @Test
    void generatedCodeHandlesExceptions() throws IOException {
        JavaFileObject source = JavaFileObjects.forSourceString("test.RiskyService", """
            package test;

            import io.axiom.persistence.tx.Transactional;
            import java.io.IOException;

            public class RiskyService {
                @Transactional
                public void riskyOperation() throws IOException, IllegalStateException {
                    throw new IOException("test");
                }
            }
            """);

        Compilation compilation = javac()
                .withProcessors(new TransactionalProcessor())
                .withOptions("--enable-preview", "--release", "25")
                .compile(source);

        assertThat(compilation).succeeded();

        var generatedFile = compilation.generatedSourceFile("test.RiskyService$Tx");
        assertThat(generatedFile).isPresent();

        String generatedCode = generatedFile.get().getCharContent(true).toString();

        // Verify exceptions are declared
        assertThat(generatedCode)
                .contains("throws IOException, IllegalStateException");
    }

    @Test
    void generatedCodeWithConstructorParameters() throws IOException {
        JavaFileObject source = JavaFileObjects.forSourceString("test.ConfigurableService", """
            package test;

            import io.axiom.persistence.tx.Transactional;

            public class ConfigurableService {
                private final String config;
                private final int maxRetries;

                public ConfigurableService(String config, int maxRetries) {
                    this.config = config;
                    this.maxRetries = maxRetries;
                }

                @Transactional
                public void execute() {
                    // uses config
                }
            }
            """);

        Compilation compilation = javac()
                .withProcessors(new TransactionalProcessor())
                .withOptions("--enable-preview", "--release", "25")
                .compile(source);

        assertThat(compilation).succeeded();

        var generatedFile = compilation.generatedSourceFile("test.ConfigurableService$Tx");
        assertThat(generatedFile).isPresent();

        String generatedCode = generatedFile.get().getCharContent(true).toString();

        // Verify constructor mirrors original + DataSource
        assertThat(generatedCode)
                .contains("public ConfigurableService$Tx(DataSource dataSource, String config, int maxRetries)")
                .contains("super(config, maxRetries)");
    }

    @Test
    void generatedCodeClassLevelAnnotation() throws IOException {
        JavaFileObject source = JavaFileObjects.forSourceString("test.FullyTransactionalService", """
            package test;

            import io.axiom.persistence.tx.Transactional;

            @Transactional
            public class FullyTransactionalService {
                public void method1() {}
                public String method2(int value) { return String.valueOf(value); }
                public int method3() { return 42; }
            }
            """);

        Compilation compilation = javac()
                .withProcessors(new TransactionalProcessor())
                .withOptions("--enable-preview", "--release", "25")
                .compile(source);

        assertThat(compilation).succeeded();

        var generatedFile = compilation.generatedSourceFile("test.FullyTransactionalService$Tx");
        assertThat(generatedFile).isPresent();

        String generatedCode = generatedFile.get().getCharContent(true).toString();

        // Verify all public methods are wrapped
        assertThat(generatedCode)
                .contains("public void method1()")
                .contains("public String method2(int value)")
                .contains("public int method3()")
                .contains("super.method1()")
                .contains("super.method2(value)")
                .contains("super.method3()");
    }
}
