package com.aicyber.backend.auth.service;

import com.aicyber.backend.auth.dto.RegisterRequest;
import com.aicyber.backend.auth.repository.AccountActionTokenRepository;
import com.aicyber.backend.auth.security.AccountTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class EmailVerificationServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private AccountActionTokenRepository tokenRepository;

    @Autowired
    private AccountTokenService tokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void registrationCreatesAnUnverifiedAccountWithOneHashedToken() {
        var response = authService.register(new RegisterRequest(uniqueEmail(), "secure-password", "Verification Test"));

        assertFalse(response.user().emailVerified());
        assertEquals(1, activeTokenCount(response.user().id()));
        String storedHash = jdbcTemplate.queryForObject(
                "SELECT token_hash FROM account_action_tokens WHERE user_id = ? AND revoked_at IS NULL",
                String.class,
                response.user().id()
        );
        assertNotNull(storedHash);
        assertEquals(64, storedHash.length());
        assertNotEquals("secure-password", storedHash);
    }

    @Test
    void confirmationIsSingleUseAndMarksTheAccountVerified() {
        var response = authService.register(new RegisterRequest(uniqueEmail(), "secure-password", "Verification Test"));
        UUID userId = response.user().id();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String rawToken = tokenService.generate();

        tokenRepository.revokeActive(userId, AccountActionTokenRepository.EMAIL_VERIFICATION, now);
        tokenRepository.create(
                UUID.randomUUID(), userId, AccountActionTokenRepository.EMAIL_VERIFICATION,
                tokenService.hash(rawToken), now.plusHours(24)
        );

        emailVerificationService.confirm(rawToken);

        assertTrue(authService.currentUser(userId).emailVerified());
        assertEquals(0, activeTokenCount(userId));
        assertThrows(IllegalArgumentException.class, () -> emailVerificationService.confirm(rawToken));
    }

    @Test
    void resendingRevokesThePreviousToken() {
        var response = authService.register(new RegisterRequest(uniqueEmail(), "secure-password", "Verification Test"));

        emailVerificationService.send(response.user().id());

        assertEquals(1, activeTokenCount(response.user().id()));
        Integer revokedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_action_tokens WHERE user_id = ? AND revoked_at IS NOT NULL",
                Integer.class,
                response.user().id()
        );
        assertEquals(1, revokedCount);
    }

    private int activeTokenCount(UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_action_tokens WHERE user_id = ? " +
                        "AND consumed_at IS NULL AND revoked_at IS NULL",
                Integer.class,
                userId
        );
    }

    private String uniqueEmail() {
        return "verification-" + UUID.randomUUID() + "@example.com";
    }
}
