package io.axiom.persistence.jdbc;

import java.sql.*;
import java.util.*;
import java.util.function.Function;

import javax.sql.DataSource;

import io.axiom.persistence.tx.Transaction;
import io.axiom.persistence.tx.TransactionContext;

/**
 * Simple JDBC helper for common database operations.
 *
 * <p>
 * This class provides a thin wrapper around JDBC that integrates with
 * Axiom's transaction management. It simplifies common patterns while
 * keeping JDBC's power available.
 *
 * <h2>Usage within a Transaction</h2>
 * <pre>{@code
 * Transaction.execute(dataSource, () -> {
 *     // Query with row mapper
 *     List<User> users = Jdbc.query(
 *         "SELECT id, name, email FROM users WHERE active = ?",
 *         rs -> new User(rs.getLong("id"), rs.getString("name"), rs.getString("email")),
 *         true
 *     );
 *
 *     // Single result
 *     Optional<User> user = Jdbc.queryOne(
 *         "SELECT * FROM users WHERE id = ?",
 *         rs -> new User(rs.getLong("id"), rs.getString("name"), rs.getString("email")),
 *         userId
 *     );
 *
 *     // Update
 *     int updated = Jdbc.update(
 *         "UPDATE users SET last_login = ? WHERE id = ?",
 *         Instant.now(), userId
 *     );
 *
 *     // Insert with generated key
 *     long id = Jdbc.insertAndReturnKey(
 *         "INSERT INTO users (name, email) VALUES (?, ?)",
 *         "John", "john@example.com"
 *     );
 * });
 * }</pre>
 *
 * <h2>Standalone Usage (without transaction)</h2>
 * <pre>{@code
 * // Using explicit connection
 * try (Connection conn = dataSource.getConnection()) {
 *     List<User> users = Jdbc.query(conn,
 *         "SELECT * FROM users",
 *         rs -> new User(...)
 *     );
 * }
 * }</pre>
 *
 * @since 0.1.0
 */
public final class Jdbc {

    private Jdbc() {}

    // ==================== Query Methods ====================

    /**
     * Executes a query and maps results using the current transaction's connection.
     *
     * @param <T> the result type
     * @param sql the SQL query
     * @param mapper function to map each row
     * @param params query parameters
     * @return list of mapped results
     */
    public static <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        return query(requireConnection(), sql, mapper, params);
    }

    /**
     * Executes a query and maps results using the provided connection.
     *
     * @param <T> the result type
     * @param connection the JDBC connection
     * @param sql the SQL query
     * @param mapper function to map each row
     * @param params query parameters
     * @return list of mapped results
     */
    public static <T> List<T> query(Connection connection, String sql,
                                    RowMapper<T> mapper, Object... params) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            throw new JdbcException("Query failed: " + sql, e);
        }
    }

    /**
     * Executes a query expecting a single result.
     *
     * @param <T> the result type
     * @param sql the SQL query
     * @param mapper function to map the row
     * @param params query parameters
     * @return optional containing the result, or empty if no rows
     */
    public static <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        return queryOne(requireConnection(), sql, mapper, params);
    }

    /**
     * Executes a query expecting a single result.
     *
     * @param <T> the result type
     * @param connection the JDBC connection
     * @param sql the SQL query
     * @param mapper function to map the row
     * @param params query parameters
     * @return optional containing the result, or empty if no rows
     */
    public static <T> Optional<T> queryOne(Connection connection, String sql,
                                           RowMapper<T> mapper, Object... params) {
        List<T> results = query(connection, sql, mapper, params);
        if (results.isEmpty()) {
            return Optional.empty();
        }
        if (results.size() > 1) {
            throw new JdbcException("Expected single result but got " + results.size());
        }
        return Optional.of(results.get(0));
    }

    /**
     * Executes a query expecting exactly one result.
     *
     * @param <T> the result type
     * @param sql the SQL query
     * @param mapper function to map the row
     * @param params query parameters
     * @return the result
     * @throws JdbcException if no result or multiple results
     */
    public static <T> T queryRequired(String sql, RowMapper<T> mapper, Object... params) {
        return queryOne(sql, mapper, params)
                .orElseThrow(() -> new JdbcException("Expected result but got none"));
    }

    /**
     * Queries for a single scalar value.
     *
     * @param <T> the value type
     * @param sql the SQL query
     * @param type the expected type
     * @param params query parameters
     * @return optional containing the value
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> queryScalar(String sql, Class<T> type, Object... params) {
        return queryOne(sql, rs -> (T) rs.getObject(1), params);
    }

    /**
     * Queries for a count.
     *
     * @param sql the SQL query (should return single numeric column)
     * @param params query parameters
     * @return the count
     */
    public static long queryCount(String sql, Object... params) {
        return queryScalar(sql, Number.class, params)
                .map(Number::longValue)
                .orElse(0L);
    }

    // ==================== Update Methods ====================

    /**
     * Executes an update (INSERT, UPDATE, DELETE) using the current transaction.
     *
     * @param sql the SQL statement
     * @param params statement parameters
     * @return number of affected rows
     */
    public static int update(String sql, Object... params) {
        return update(requireConnection(), sql, params);
    }

    /**
     * Executes an update using the provided connection.
     *
     * @param connection the JDBC connection
     * @param sql the SQL statement
     * @param params statement parameters
     * @return number of affected rows
     */
    public static int update(Connection connection, String sql, Object... params) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, params);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new JdbcException("Update failed: " + sql, e);
        }
    }

    /**
     * Executes an insert and returns the generated key.
     *
     * @param sql the INSERT statement
     * @param params statement parameters
     * @return the generated key
     */
    public static long insertAndReturnKey(String sql, Object... params) {
        return insertAndReturnKey(requireConnection(), sql, params);
    }

    /**
     * Executes an insert and returns the generated key.
     *
     * @param connection the JDBC connection
     * @param sql the INSERT statement
     * @param params statement parameters
     * @return the generated key
     */
    public static long insertAndReturnKey(Connection connection, String sql, Object... params) {
        try (PreparedStatement stmt = connection.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            setParameters(stmt, params);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new JdbcException("No generated key returned");
            }
        } catch (SQLException e) {
            throw new JdbcException("Insert failed: " + sql, e);
        }
    }

    // ==================== Batch Methods ====================

    /**
     * Executes a batch update.
     *
     * @param sql the SQL statement
     * @param batchParams list of parameter arrays
     * @return array of update counts
     */
    public static int[] batchUpdate(String sql, List<Object[]> batchParams) {
        return batchUpdate(requireConnection(), sql, batchParams);
    }

    /**
     * Executes a batch update.
     *
     * @param connection the JDBC connection
     * @param sql the SQL statement
     * @param batchParams list of parameter arrays
     * @return array of update counts
     */
    public static int[] batchUpdate(Connection connection, String sql, List<Object[]> batchParams) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (Object[] params : batchParams) {
                setParameters(stmt, params);
                stmt.addBatch();
            }
            return stmt.executeBatch();
        } catch (SQLException e) {
            throw new JdbcException("Batch update failed: " + sql, e);
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Gets the connection from the current transaction context.
     *
     * @return the current transaction's connection
     * @throws JdbcException if no transaction is active
     */
    public static Connection requireConnection() {
        return TransactionContext.current()
                .map(TransactionContext::connection)
                .orElseThrow(() -> new JdbcException(
                    "No active transaction. Use Transaction.execute() or provide a connection."
                ));
    }

    /**
     * Executes a function with a new connection from the data source.
     * The connection is auto-committed and closed after the function completes.
     *
     * @param <T> the return type
     * @param dataSource the data source
     * @param action the function to execute
     * @return the function result
     */
    public static <T> T withConnection(DataSource dataSource,
                                       Function<Connection, T> action) {
        try (Connection conn = dataSource.getConnection()) {
            return action.apply(conn);
        } catch (SQLException e) {
            throw new JdbcException("Failed to execute with connection", e);
        }
    }

    // ==================== Helper Methods ====================

    private static void setParameters(PreparedStatement stmt, Object... params)
            throws SQLException {
        for (int i = 0; i < params.length; i++) {
            setParameter(stmt, i + 1, params[i]);
        }
    }

    private static void setParameter(PreparedStatement stmt, int index, Object value)
            throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.NULL);
        } else if (value instanceof String s) {
            stmt.setString(index, s);
        } else if (value instanceof Integer i) {
            stmt.setInt(index, i);
        } else if (value instanceof Long l) {
            stmt.setLong(index, l);
        } else if (value instanceof Double d) {
            stmt.setDouble(index, d);
        } else if (value instanceof Float f) {
            stmt.setFloat(index, f);
        } else if (value instanceof Boolean b) {
            stmt.setBoolean(index, b);
        } else if (value instanceof java.time.Instant instant) {
            stmt.setTimestamp(index, Timestamp.from(instant));
        } else if (value instanceof java.time.LocalDate date) {
            stmt.setDate(index, java.sql.Date.valueOf(date));
        } else if (value instanceof java.time.LocalDateTime dateTime) {
            stmt.setTimestamp(index, Timestamp.valueOf(dateTime));
        } else if (value instanceof java.time.LocalTime time) {
            stmt.setTime(index, Time.valueOf(time));
        } else if (value instanceof byte[] bytes) {
            stmt.setBytes(index, bytes);
        } else if (value instanceof java.util.UUID uuid) {
            stmt.setObject(index, uuid);
        } else if (value instanceof Enum<?> e) {
            stmt.setString(index, e.name());
        } else {
            stmt.setObject(index, value);
        }
    }

    // ==================== Functional Interfaces ====================

    /**
     * Maps a ResultSet row to an object.
     *
     * @param <T> the result type
     */
    @FunctionalInterface
    public interface RowMapper<T> {
        /**
         * Maps the current row of the ResultSet to an object.
         *
         * @param rs the result set positioned at the current row
         * @return the mapped object
         * @throws SQLException if a database access error occurs
         */
        T map(ResultSet rs) throws SQLException;
    }
}
