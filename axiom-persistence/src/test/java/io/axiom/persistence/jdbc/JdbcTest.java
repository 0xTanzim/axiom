package io.axiom.persistence.jdbc;

import io.axiom.persistence.tx.Transaction;
import io.axiom.persistence.tx.TransactionContext;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JdbcTest {

    private static DataSource dataSource;

    @BeforeAll
    static void setup() throws SQLException {
        dataSource = createH2DataSource();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    email VARCHAR(255),
                    active BOOLEAN DEFAULT TRUE
                )
                """);
        }
    }

    @AfterAll
    static void teardown() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE users");
        }
    }

    @BeforeEach
    void cleanTable() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM users");
        }
    }

    @Test
    void queryReturnsResults() {
        // Insert test data
        Transaction.execute(dataSource, () -> {
            Jdbc.update("INSERT INTO users (name, email) VALUES (?, ?)", "John", "john@example.com");
            Jdbc.update("INSERT INTO users (name, email) VALUES (?, ?)", "Jane", "jane@example.com");
        });

        // Query
        List<TestUser> users = Transaction.execute(dataSource, () -> {
            return Jdbc.query(
                "SELECT id, name, email FROM users ORDER BY name",
                rs -> new TestUser(rs.getLong("id"), rs.getString("name"), rs.getString("email"))
            );
        });

        assertEquals(2, users.size());
        assertEquals("Jane", users.get(0).name());
        assertEquals("John", users.get(1).name());
    }

    @Test
    void queryOneReturnsSingleResult() {
        Transaction.execute(dataSource, () -> {
            Jdbc.update("INSERT INTO users (name, email) VALUES (?, ?)", "John", "john@example.com");
        });

        Optional<TestUser> user = Transaction.execute(dataSource, () -> {
            return Jdbc.queryOne(
                "SELECT id, name, email FROM users WHERE name = ?",
                rs -> new TestUser(rs.getLong("id"), rs.getString("name"), rs.getString("email")),
                "John"
            );
        });

        assertTrue(user.isPresent());
        assertEquals("John", user.get().name());
    }

    @Test
    void queryOneReturnsEmptyWhenNotFound() {
        Optional<TestUser> user = Transaction.execute(dataSource, () -> {
            return Jdbc.queryOne(
                "SELECT id, name, email FROM users WHERE name = ?",
                rs -> new TestUser(rs.getLong("id"), rs.getString("name"), rs.getString("email")),
                "NonExistent"
            );
        });

        assertTrue(user.isEmpty());
    }

    @Test
    void queryOneThrowsOnMultipleResults() {
        Transaction.execute(dataSource, () -> {
            Jdbc.update("INSERT INTO users (name, email) VALUES (?, ?)", "John", "john1@example.com");
            Jdbc.update("INSERT INTO users (name, email) VALUES (?, ?)", "John", "john2@example.com");
        });

        assertThrows(JdbcException.class, () -> {
            Transaction.execute(dataSource, () -> {
                return Jdbc.queryOne(
                    "SELECT id, name, email FROM users WHERE name = ?",
                    rs -> new TestUser(rs.getLong("id"), rs.getString("name"), rs.getString("email")),
                    "John"
                );
            });
        });
    }

    @Test
    void queryRequiredThrowsWhenNotFound() {
        assertThrows(JdbcException.class, () -> {
            Transaction.execute(dataSource, () -> {
                return Jdbc.queryRequired(
                    "SELECT id, name, email FROM users WHERE name = ?",
                    rs -> new TestUser(rs.getLong("id"), rs.getString("name"), rs.getString("email")),
                    "NonExistent"
                );
            });
        });
    }

    @Test
    void queryScalarReturnsValue() {
        Transaction.execute(dataSource, () -> {
            Jdbc.update("INSERT INTO users (name, email) VALUES (?, ?)", "John", "john@example.com");
            Jdbc.update("INSERT INTO users (name, email) VALUES (?, ?)", "Jane", "jane@example.com");
        });

        long count = Transaction.execute(dataSource, () -> {
            return Jdbc.queryCount("SELECT COUNT(*) FROM users");
        });

        assertEquals(2, count);
    }

    @Test
    void updateReturnsAffectedRows() {
        Transaction.execute(dataSource, () -> {
            Jdbc.update("INSERT INTO users (name, email) VALUES (?, ?)", "John", "john@example.com");
            Jdbc.update("INSERT INTO users (name, email) VALUES (?, ?)", "Jane", "jane@example.com");
        });

        int updated = Transaction.execute(dataSource, () -> {
            return Jdbc.update("UPDATE users SET active = ?", false);
        });

        assertEquals(2, updated);
    }

    @Test
    void insertAndReturnKeyReturnsGeneratedId() {
        long id = Transaction.execute(dataSource, () -> {
            return Jdbc.insertAndReturnKey(
                "INSERT INTO users (name, email) VALUES (?, ?)",
                "John", "john@example.com"
            );
        });

        assertTrue(id > 0);
    }

    @Test
    void batchUpdateExecutesMultipleStatements() {
        int[] results = Transaction.execute(dataSource, () -> {
            return Jdbc.batchUpdate(
                "INSERT INTO users (name, email) VALUES (?, ?)",
                List.of(
                    new Object[]{"John", "john@example.com"},
                    new Object[]{"Jane", "jane@example.com"},
                    new Object[]{"Bob", "bob@example.com"}
                )
            );
        });

        assertEquals(3, results.length);

        long count = Transaction.execute(dataSource, () ->
            Jdbc.queryCount("SELECT COUNT(*) FROM users")
        );
        assertEquals(3, count);
    }

    @Test
    void requireConnectionThrowsOutsideTransaction() {
        assertThrows(JdbcException.class, Jdbc::requireConnection);
    }

    @Test
    void queryWithConnectionWorksOutsideTransaction() throws SQLException {
        // Insert some data first
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(true);
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO users (name, email) VALUES (?, ?)")) {
                stmt.setString(1, "John");
                stmt.setString(2, "john@example.com");
                stmt.executeUpdate();
            }
        }

        // Query without transaction
        try (Connection conn = dataSource.getConnection()) {
            List<TestUser> users = Jdbc.query(
                conn,
                "SELECT id, name, email FROM users",
                rs -> new TestUser(rs.getLong("id"), rs.getString("name"), rs.getString("email"))
            );
            assertEquals(1, users.size());
        }
    }

    @Test
    void handlesNullParameters() {
        Transaction.execute(dataSource, () -> {
            Jdbc.update("INSERT INTO users (name, email) VALUES (?, ?)", "John", null);
        });

        Optional<TestUser> user = Transaction.execute(dataSource, () -> {
            return Jdbc.queryOne(
                "SELECT id, name, email FROM users WHERE name = ?",
                rs -> new TestUser(rs.getLong("id"), rs.getString("name"), rs.getString("email")),
                "John"
            );
        });

        assertTrue(user.isPresent());
        assertNull(user.get().email());
    }

    private static DataSource createH2DataSource() {
        org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
        ds.setURL("jdbc:h2:mem:jdbctest;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    record TestUser(long id, String name, String email) {}
}
