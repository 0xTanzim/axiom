package io.axiom.persistence;

import io.axiom.persistence.config.PersistenceConfig;
import io.axiom.persistence.jdbc.Jdbc;
import io.axiom.persistence.tx.Transaction;
import org.junit.jupiter.api.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

class AxiomPersistenceTest {

    private AxiomPersistence persistence;

    @AfterEach
    void cleanup() {
        if (persistence != null && persistence.isRunning()) {
            persistence.stopInstance();
        }
        // Also cleanup global instance
        if (AxiomPersistence.isStarted()) {
            AxiomPersistence.stop();
        }
    }

    // ==================== Instance API Tests ====================

    @Test
    void startAndStopLifecycle() {
        persistence = createTestPersistence();

        assertFalse(persistence.isRunning());
        assertEquals("NEW", persistence.state());

        persistence.startInstance();

        assertTrue(persistence.isRunning());
        assertEquals("RUNNING", persistence.state());
        assertNotNull(persistence.dataSource());

        persistence.stopInstance();

        assertFalse(persistence.isRunning());
        assertEquals("STOPPED", persistence.state());
    }

    @Test
    void cannotStartTwice() {
        persistence = createTestPersistence();
        persistence.startInstance();

        assertThrows(PersistenceException.class, persistence::startInstance);
    }

    @Test
    void cannotAccessDataSourceBeforeStart() {
        persistence = createTestPersistence();

        assertThrows(PersistenceException.class, persistence::dataSource);
    }

    @Test
    void transactionsWorkWithManagedDataSource() throws SQLException {
        persistence = createTestPersistence();
        persistence.startInstance();

        // Create test table
        try (Connection conn = persistence.dataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE items (id BIGINT PRIMARY KEY, name VARCHAR(255))");
        }

        // Use transaction API with explicit DataSource
        Transaction.execute(persistence.dataSource(), () -> {
            Jdbc.update("INSERT INTO items (id, name) VALUES (?, ?)", 1L, "Test Item");
        });

        // Verify
        long count = Transaction.execute(persistence.dataSource(), () -> {
            return Jdbc.queryCount("SELECT COUNT(*) FROM items");
        });

        assertEquals(1, count);

        // Cleanup
        try (Connection conn = persistence.dataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE items");
        }
    }

    @Test
    void configReturnsProvidedConfig() {
        PersistenceConfig config = PersistenceConfig.builder()
                .url("jdbc:h2:mem:configtest")
                .username("sa")
                .password("")
                .build();

        persistence = AxiomPersistence.builder()
                .config(config)
                .build();

        assertSame(config, persistence.config());
    }

    @Test
    void builderRequiresConfig() {
        assertThrows(NullPointerException.class, () -> {
            AxiomPersistence.builder().build();
        });
    }

    @Test
    void createFactoryMethodLoadsConfig() {
        persistence = AxiomPersistence.create(
            PersistenceConfig.builder()
                .url("jdbc:h2:mem:factorytest")
                .username("sa")
                .password("")
                .build()
        );

        assertNotNull(persistence);
        assertNotNull(persistence.config());
    }

    // ==================== Static Global API Tests (New DX) ====================

    @Test
    void staticStartAndStopWorks() {
        AxiomPersistence.start(testConfig());

        assertTrue(AxiomPersistence.isStarted());
        assertNotNull(AxiomPersistence.globalDataSource());
        assertNotNull(AxiomPersistence.instance());

        AxiomPersistence.stop();

        assertFalse(AxiomPersistence.isStarted());
    }

    @Test
    void staticStartCannotBeCalledTwice() {
        AxiomPersistence.start(testConfig());

        assertThrows(PersistenceException.class, () ->
            AxiomPersistence.start(testConfig()));
    }

    @Test
    void globalDataSourceThrowsIfNotStarted() {
        assertThrows(PersistenceException.class, AxiomPersistence::globalDataSource);
    }

    @Test
    void transactionExecuteWorksWithGlobalDataSource() throws SQLException {
        AxiomPersistence.start(testConfig());

        // Create test table
        try (Connection conn = AxiomPersistence.globalDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE global_test (id BIGINT PRIMARY KEY, name VARCHAR(255))");
        }

        // NEW DX: Transaction.execute() without DataSource!
        Transaction.execute(() -> {
            Jdbc.update("INSERT INTO global_test (id, name) VALUES (?, ?)", 1L, "Global Test");
        });

        // Verify with simplified API
        long count = Transaction.execute(() -> {
            return Jdbc.queryCount("SELECT COUNT(*) FROM global_test");
        });

        assertEquals(1, count);

        // Cleanup
        try (Connection conn = AxiomPersistence.globalDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE global_test");
        }
    }

    @Test
    void transactionBuilderWorksWithGlobalDataSource() throws SQLException {
        AxiomPersistence.start(testConfig());

        // Create test table
        try (Connection conn = AxiomPersistence.globalDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE builder_test (id BIGINT PRIMARY KEY)");
        }

        // NEW DX: Transaction.builder() without DataSource!
        Transaction.builder()
            .readOnly(false)
            .execute(() -> {
                Jdbc.update("INSERT INTO builder_test (id) VALUES (?)", 1L);
            });

        // Cleanup
        try (Connection conn = AxiomPersistence.globalDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE builder_test");
        }
    }

    // ==================== Helpers ====================

    private AxiomPersistence createTestPersistence() {
        return AxiomPersistence.builder()
                .config(testConfig())
                .build();
    }

    private static PersistenceConfig testConfig() {
        return PersistenceConfig.builder()
                .url("jdbc:h2:mem:test" + System.nanoTime())
                .username("sa")
                .password("")
                .maximumPoolSize(2)
                .minimumIdle(1)
                .build();
    }
}
