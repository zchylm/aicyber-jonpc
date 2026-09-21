package com.aicyber.backend.auth.repository;

import com.aicyber.backend.auth.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByEmail(String email) {
        return jdbcTemplate.query(
                "SELECT id, email, password_hash, display_name, role, status, email_verified_at, auth_version, created_at, updated_at " +
                        "FROM users WHERE email = ?",
                (resultSet, rowNum) -> new User(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("email"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("display_name"),
                        resultSet.getString("role"),
                        resultSet.getString("status"),
                        resultSet.getObject("email_verified_at", OffsetDateTime.class),
                        resultSet.getInt("auth_version"),
                        resultSet.getObject("created_at", OffsetDateTime.class),
                        resultSet.getObject("updated_at", OffsetDateTime.class)
                ),
                email
        ).stream().findFirst();
    }

    public Optional<User> findById(UUID id) {
        return jdbcTemplate.query(
                "SELECT id, email, password_hash, display_name, role, status, email_verified_at, auth_version, created_at, updated_at " +
                        "FROM users WHERE id = ?",
                (resultSet, rowNum) -> new User(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("email"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("display_name"),
                        resultSet.getString("role"),
                        resultSet.getString("status"),
                        resultSet.getObject("email_verified_at", OffsetDateTime.class),
                        resultSet.getInt("auth_version"),
                        resultSet.getObject("created_at", OffsetDateTime.class),
                        resultSet.getObject("updated_at", OffsetDateTime.class)
                ),
                id
        ).stream().findFirst();
    }

    public User create(UUID id, String email, String passwordHash, String displayName, OffsetDateTime emailVerifiedAt) {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, display_name, email_verified_at) VALUES (?, ?, ?, ?, ?)",
                id, email, passwordHash, displayName, emailVerifiedAt
        );
        return findByEmail(email).orElseThrow();
    }

    public Optional<User> lockById(UUID id) {
        return jdbcTemplate.query(
                "SELECT id, email, password_hash, display_name, role, status, email_verified_at, auth_version, created_at, updated_at " +
                        "FROM users WHERE id = ? FOR UPDATE",
                (resultSet, rowNum) -> new User(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("email"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("display_name"),
                        resultSet.getString("role"),
                        resultSet.getString("status"),
                        resultSet.getObject("email_verified_at", OffsetDateTime.class),
                        resultSet.getInt("auth_version"),
                        resultSet.getObject("created_at", OffsetDateTime.class),
                        resultSet.getObject("updated_at", OffsetDateTime.class)
                ),
                id
        ).stream().findFirst();
    }

    public void markEmailVerified(UUID id, OffsetDateTime verifiedAt) {
        jdbcTemplate.update(
                "UPDATE users SET email_verified_at = COALESCE(email_verified_at, ?), updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                verifiedAt, id
        );
    }

    public void updatePassword(UUID id, String passwordHash) {
        int updated = jdbcTemplate.update(
                "UPDATE users SET password_hash = ?, auth_version = auth_version + 1, " +
                        "updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                passwordHash, id
        );
        if (updated != 1) {
            throw new IllegalArgumentException("User account not found");
        }
    }
}
