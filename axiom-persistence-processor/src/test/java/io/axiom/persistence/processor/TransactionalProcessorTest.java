package io.axiom.persistence.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class TransactionalProcessorTest {

    @Test
    void processesSimpleTransactionalMethod() {
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
        assertThat(compilation).generatedSourceFile("test.OrderService$Tx");
    }

    @Test
    void processesMethodWithReturnValue() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.UserService", """
            package test;

            import io.axiom.persistence.tx.Transactional;
            import java.util.List;

            public class UserService {
                @Transactional(readOnly = true)
                public List<String> findAllUsers() {
                    return List.of();
                }
            }
            """);

        Compilation compilation = javac()
                .withProcessors(new TransactionalProcessor())
                .withOptions("--enable-preview", "--release", "25")
                .compile(source);

        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("test.UserService$Tx");
    }

    @Test
    void processesMethodWithAllOptions() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.PaymentService", """
            package test;

            import io.axiom.persistence.tx.Transactional;
            import io.axiom.persistence.tx.IsolationLevel;
            import io.axiom.persistence.tx.Propagation;

            public class PaymentService {
                @Transactional(
                    isolation = IsolationLevel.SERIALIZABLE,
                    propagation = Propagation.REQUIRES_NEW,
                    readOnly = false,
                    timeout = 30,
                    label = "process-payment"
                )
                public void processPayment(String paymentId) {
                    // payment logic
                }
            }
            """);

        Compilation compilation = javac()
                .withProcessors(new TransactionalProcessor())
                .withOptions("--enable-preview", "--release", "25")
                .compile(source);

        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("test.PaymentService$Tx");
    }

    @Test
    void processesClassLevelAnnotation() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.InventoryService", """
            package test;

            import io.axiom.persistence.tx.Transactional;

            @Transactional
            public class InventoryService {
                public void addItem(String item) {
                    // add logic
                }

                public int getCount() {
                    return 0;
                }
            }
            """);

        Compilation compilation = javac()
                .withProcessors(new TransactionalProcessor())
                .withOptions("--enable-preview", "--release", "25")
                .compile(source);

        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("test.InventoryService$Tx");
    }

    @Test
    void rejectsPrivateMethod() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.BadService", """
            package test;

            import io.axiom.persistence.tx.Transactional;

            public class BadService {
                @Transactional
                private void privateMethod() {
                    // should fail
                }
            }
            """);

        Compilation compilation = javac()
                .withProcessors(new TransactionalProcessor())
                .withOptions("--enable-preview", "--release", "25")
                .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("must be overridable");
    }

    @Test
    void rejectsStaticMethod() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.BadService2", """
            package test;

            import io.axiom.persistence.tx.Transactional;

            public class BadService2 {
                @Transactional
                public static void staticMethod() {
                    // should fail
                }
            }
            """);

        Compilation compilation = javac()
                .withProcessors(new TransactionalProcessor())
                .withOptions("--enable-preview", "--release", "25")
                .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("must be overridable");
    }

    @Test
    void rejectsFinalMethod() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.BadService3", """
            package test;

            import io.axiom.persistence.tx.Transactional;

            public class BadService3 {
                @Transactional
                public final void finalMethod() {
                    // should fail
                }
            }
            """);

        Compilation compilation = javac()
                .withProcessors(new TransactionalProcessor())
                .withOptions("--enable-preview", "--release", "25")
                .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("must be overridable");
    }

    @Test
    void rejectsFinalClass() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.FinalService", """
            package test;

            import io.axiom.persistence.tx.Transactional;

            public final class FinalService {
                @Transactional
                public void someMethod() {
                    // should fail - class is final
                }
            }
            """);

        Compilation compilation = javac()
                .withProcessors(new TransactionalProcessor())
                .withOptions("--enable-preview", "--release", "25")
                .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("final classes");
    }

    @Test
    void handlesConstructorWithParameters() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.ServiceWithDeps", """
            package test;

            import io.axiom.persistence.tx.Transactional;

            public class ServiceWithDeps {
                private final String config;

                public ServiceWithDeps(String config) {
                    this.config = config;
                }

                @Transactional
                public void doSomething() {
                    // uses config
                }
            }
            """);

        Compilation compilation = javac()
                .withProcessors(new TransactionalProcessor())
                .withOptions("--enable-preview", "--release", "25")
                .compile(source);

        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("test.ServiceWithDeps$Tx");
    }

    @Test
    void handlesMethodWithMultipleParameters() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.MultiParamService", """
            package test;

            import io.axiom.persistence.tx.Transactional;

            public class MultiParamService {
                @Transactional
                public String process(String id, int count, boolean flag) {
                    return id;
                }
            }
            """);

        Compilation compilation = javac()
                .withProcessors(new TransactionalProcessor())
                .withOptions("--enable-preview", "--release", "25")
                .compile(source);

        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("test.MultiParamService$Tx");
    }

    @Test
    void handlesMethodWithExceptions() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.ExceptionService", """
            package test;

            import io.axiom.persistence.tx.Transactional;
            import java.io.IOException;

            public class ExceptionService {
                @Transactional
                public void riskyMethod() throws IOException, IllegalStateException {
                    // might throw
                }
            }
            """);

        Compilation compilation = javac()
                .withProcessors(new TransactionalProcessor())
                .withOptions("--enable-preview", "--release", "25")
                .compile(source);

        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("test.ExceptionService$Tx");
    }
}
