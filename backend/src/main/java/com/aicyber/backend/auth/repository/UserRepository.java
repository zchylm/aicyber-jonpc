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
                "SELECT id, email, password_hash, display_name, role, status, created_at, updated_at " +
                        "FROM users WHERE email = ?",
                (resultSet, rowNum) -> new User(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("email"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("display_name"),
                        resultSet.getString("role"),
                        resultSet.getString("status"),
                        resultSet.getObject("created_at", OffsetDateTime.class),
                        resultSet.getObject("updated_at", OffsetDateTime.class)
                ),
                email
        ).stream().findFirst();
    }

    public Optional<User> findById(UUID id) {
        return jdbcTemplate.query(
                "SELECT id, email, password_hash, display_name, role, status, created_at, updated_at " +
                        "FROM users WHERE id = ?",
                (resultSet, rowNum) -> new User(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("email"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("display_name"),
                        resultSet.getString("role"),
                        resultSet.getString("status"),
                        resultSet.getObject("created_at", OffsetDateTime.class),
                        resultSet.getObject("updated_at", OffsetDateTime.class)
                ),
                id
        ).stream().findFirst();
    }

    public User create(UUID id, String email, String passwordHash, String displayName) {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, ?, ?)",
                id, email, passwordHash, displayName
        );
        return findByEmail(email).orElseThrow();
    }
}
