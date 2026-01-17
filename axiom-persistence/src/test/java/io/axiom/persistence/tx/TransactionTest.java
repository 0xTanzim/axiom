package io.axiom.persistence.tx;

import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    private static DataSource dataSource;

    @BeforeAll
    static void setup() throws SQLException {
        // Create H2 in-memory database
        dataSource = createH2DataSource();

        // Create test table
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE test_users (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255))");
        }
    }

    @AfterAll
    static void teardown() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE test_users");
        }
    }

    @BeforeEach
    void cleanTable() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM test_users");
        }
    }

    @Test
    void executeCommitsOnSuccess() throws SQLException {
        Transaction.execute(dataSource, () -> {
            Connection conn = TransactionContext.require().connection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO test_users (name) VALUES (?)")) {
                stmt.setString(1, "John");
                stmt.executeUpdate();
            }
        });

        // Verify data was committed
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM test_users")) {
            rs.next();
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void executeRollsBackOnException() throws SQLException {
        assertThrows(RuntimeException.class, () -> {
            Transaction.execute(dataSource, () -> {
                Connection conn = TransactionContext.require().connection();
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO test_users (name) VALUES (?)")) {
                    stmt.setString(1, "John");
                    stmt.executeUpdate();
                }
                throw new RuntimeException("Test failure");
            });
        });

        // Verify data was rolled back
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM test_users")) {
            rs.next();
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    void executeReturnsValue() {
        String result = Transaction.execute(dataSource, () -> {
            return "success";
        });

        assertEquals("success", result);
    }

    @Test
    void transactionContextIsActiveWithinTransaction() {
        Transaction.execute(dataSource, () -> {
            assertTrue(TransactionContext.isActive());
            assertNotNull(TransactionContext.require());
            assertNotNull(TransactionContext.require().connection());
        });
    }

    @Test
    void transactionContextIsNotActiveOutsideTransaction() {
        assertFalse(TransactionContext.isActive());
        assertTrue(TransactionContext.current().isEmpty());
    }

    @Test
    void setRollbackOnlyCausesRollback() throws SQLException {
        Transaction.execute(dataSource, () -> {
            Connection conn = TransactionContext.require().connection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO test_users (name) VALUES (?)")) {
                stmt.setString(1, "John");
                stmt.executeUpdate();
            }
            TransactionContext.require().setRollbackOnly();
        });

        // Verify data was rolled back
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM test_users")) {
            rs.next();
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    void builderSetsIsolationLevel() {
        Transaction.builder(dataSource)
                .isolation(IsolationLevel.SERIALIZABLE)
                .execute(() -> {
                    TransactionContext ctx = TransactionContext.require();
                    assertEquals(IsolationLevel.SERIALIZABLE, ctx.isolationLevel());
                });
    }

    @Test
    void builderSetsReadOnly() {
        Transaction.builder(dataSource)
                .readOnly(true)
                .execute(() -> {
                    TransactionContext ctx = TransactionContext.require();
                    assertTrue(ctx.isReadOnly());
                });
    }

    @Test
    void builderSetsName() {
        Transaction.builder(dataSource)
                .name("test-transaction")
                .execute(() -> {
                    TransactionContext ctx = TransactionContext.require();
                    assertEquals("test-transaction", ctx.name());
                });
    }

    @Test
    void propagationRequiredJoinsExistingTransaction() {
        Transaction.execute(dataSource, () -> {
            TransactionContext outer = TransactionContext.require();

            Transaction.builder(dataSource)
                    .propagation(Propagation.REQUIRED)
                    .execute(() -> {
                        TransactionContext inner = TransactionContext.require();
                        // Should be same transaction
                        assertSame(outer, inner);
                    });
        });
    }

    @Test
    void propagationMandatoryThrowsWithoutTransaction() {
        assertThrows(TransactionException.class, () -> {
            Transaction.builder(dataSource)
                    .propagation(Propagation.MANDATORY)
                    .execute(() -> {});
        });
    }

    @Test
    void propagationNeverThrowsWithTransaction() {
        Transaction.execute(dataSource, () -> {
            assertThrows(TransactionException.class, () -> {
                Transaction.builder(dataSource)
                        .propagation(Propagation.NEVER)
                        .execute(() -> {});
            });
        });
    }

    @Test
    void propagationSupportsWorksWithoutTransaction() {
        // Should not throw
        Transaction.builder(dataSource)
                .propagation(Propagation.SUPPORTS)
                .execute(() -> {
                    // No transaction context outside
                    assertFalse(TransactionContext.isActive());
                });
    }

    private static DataSource createH2DataSource() {
        org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
        ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }
}
