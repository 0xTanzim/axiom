package com.example.auth.repository;

import com.example.auth.domain.User;
import io.axiom.persistence.jdbc.Jdbc;
import io.axiom.persistence.tx.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

/**
 * User repository for database operations.
 *
 * <p>Uses Axiom's JDBC utility for clean, explicit database access.
 * Transactions are explicit using {@link Transaction#execute}.
 *
 * <p>Note: This repository uses the global DataSource configured via
 * {@code AxiomPersistence.start()}. No manual DataSource injection needed.
 */
public class UserRepository {

    private static final Logger LOG = LoggerFactory.getLogger(UserRepository.class);

    /**
     * Initialize database schema.
     */
    public void initSchema() {
        String sql = """
                CREATE TABLE IF NOT EXISTS users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    email VARCHAR(255) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    name VARCHAR(100) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """;
        Transaction.execute(() -> {
            Jdbc.update(sql);
        });
        LOG.info("Database schema initialized");
    }

    /**
     * Saves a new user and returns it with generated ID.
     */
    public User save(User user) {
        return Transaction.execute(() -> {
            String sql = """
                    INSERT INTO users (email, password_hash, name, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?)
                    """;

            long id = Jdbc.insertAndReturnKey(sql,
                    user.email(),
                    user.passwordHash(),
                    user.name(),
                    Timestamp.from(user.createdAt()),
                    Timestamp.from(user.updatedAt())
            );

            LOG.debug("Saved user with id={}", id);
            return user.withId(id);
        });
    }

    /**
     * Finds a user by email.
     */
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        return Jdbc.queryOne(sql, this::mapRow, email);
    }

    /**
     * Finds a user by ID.
     */
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        return Jdbc.queryOne(sql, this::mapRow, id);
    }

    /**
     * Updates a user.
     */
    public User update(User user) {
        return Transaction.execute(() -> {
            String sql = """
                    UPDATE users SET name = ?, updated_at = ?
                    WHERE id = ?
                    """;

            Jdbc.update(sql,
                    user.name(),
                    Timestamp.from(user.updatedAt()),
                    user.id()
            );

            LOG.debug("Updated user id={}", user.id());
            return user;
        });
    }

    /**
     * Checks if an email is already taken.
     */
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("name"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }
}
