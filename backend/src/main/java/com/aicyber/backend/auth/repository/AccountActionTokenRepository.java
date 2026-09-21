package com.aicyber.backend.auth.repository;

import com.aicyber.backend.auth.model.AccountActionToken;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AccountActionTokenRepository {

    public static final String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
    public static final String PASSWORD_RESET = "PASSWORD_RESET";

    private final JdbcTemplate jdbcTemplate;

    public AccountActionTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void revokeActive(UUID userId, String purpose, OffsetDateTime revokedAt) {
        jdbcTemplate.update(
                "UPDATE account_action_tokens SET revoked_at = ? " +
                        "WHERE user_id = ? AND purpose = ? AND consumed_at IS NULL AND revoked_at IS NULL",
                revokedAt, userId, purpose
        );
    }

    public void create(UUID id, UUID userId, String purpose, String tokenHash, OffsetDateTime expiresAt) {
        jdbcTemplate.update(
                "INSERT INTO account_action_tokens (id, user_id, purpose, token_hash, expires_at) VALUES (?, ?, ?, ?, ?)",
                id, userId, purpose, tokenHash, expiresAt
        );
    }

    public boolean existsCreatedSince(UUID userId, String purpose, OffsetDateTime createdSince) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_action_tokens WHERE user_id = ? AND purpose = ? AND created_at >= ?",
                Integer.class,
                userId, purpose, createdSince
        );
        return count != null && count > 0;
    }

    public Optional<AccountActionToken> findUsableForUpdate(String tokenHash, String purpose, OffsetDateTime now) {
        return jdbcTemplate.query(
                "SELECT id, user_id, purpose, expires_at FROM account_action_tokens " +
                        "WHERE token_hash = ? AND purpose = ? AND expires_at > ? " +
                        "AND consumed_at IS NULL AND revoked_at IS NULL FOR UPDATE",
                (resultSet, rowNum) -> new AccountActionToken(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("user_id", UUID.class),
                        resultSet.getString("purpose"),
                        resultSet.getObject("expires_at", OffsetDateTime.class)
                ),
                tokenHash, purpose, now
        ).stream().findFirst();
    }

    public void markConsumed(UUID id, OffsetDateTime consumedAt) {
        int updated = jdbcTemplate.update(
                "UPDATE account_action_tokens SET consumed_at = ? " +
                        "WHERE id = ? AND consumed_at IS NULL AND revoked_at IS NULL",
                consumedAt, id
        );
        if (updated != 1) {
            throw new IllegalArgumentException("This account link is no longer valid");
        }
    }
}
