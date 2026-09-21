package com.aicyber.backend.auth.service;

import com.aicyber.backend.auth.dto.LoginRequest;
import com.aicyber.backend.auth.dto.RegisterRequest;
import com.aicyber.backend.auth.repository.AccountActionTokenRepository;
import com.aicyber.backend.auth.repository.UserRepository;
import com.aicyber.backend.auth.security.AccountTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("local")
@AutoConfigureMockMvc
@Transactional
class PasswordResetServiceIntegrationTest {

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountActionTokenRepository tokenRepository;

    @Autowired
    private AccountTokenService tokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void requestUsesTheSameResponseForKnownAndUnknownEmails() {
        String email = uniqueEmail();
        authService.register(new RegisterRequest(email, "original-password", "Reset Test"));

        assertEquals(PasswordResetService.GENERIC_RESPONSE, passwordResetService.request(email));
        assertEquals(PasswordResetService.GENERIC_RESPONSE, passwordResetService.request(uniqueEmail()));
    }

    @Test
    void requestCreatesOneActiveTokenAndAppliesTheCooldown() {
        var account = authService.register(new RegisterRequest(
                uniqueEmail(), "original-password", "Reset Test"
        ));

        passwordResetService.request(account.user().email());
        passwordResetService.request(account.user().email());

        assertEquals(1, tokenCount(account.user().id(), false));
    }

    @Test
    void resetIsSingleUseChangesThePasswordAndInvalidatesTheOldJwt() throws Exception {
        var account = authService.register(new RegisterRequest(
                uniqueEmail(), "original-password", "Reset Test"
        ));
        String rawToken = createResetToken(account.user().id());

        passwordResetService.reset(rawToken, "replacement-password", "replacement-password");

        assertThrows(IllegalArgumentException.class, () -> authService.login(
                new LoginRequest(account.user().email(), "original-password")
        ));
        var newLogin = authService.login(new LoginRequest(account.user().email(), "replacement-password"));
        assertEquals(2, userRepository.findById(account.user().id()).orElseThrow().authVersion());
        assertEquals(0, tokenCount(account.user().id(), false));
        assertThrows(IllegalArgumentException.class, () -> passwordResetService.reset(
                rawToken, "another-password", "another-password"
        ));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + account.accessToken()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + newLogin.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void mismatchedPasswordsDoNotConsumeTheToken() {
        var account = authService.register(new RegisterRequest(
                uniqueEmail(), "original-password", "Reset Test"
        ));
        String rawToken = createResetToken(account.user().id());

        assertThrows(IllegalArgumentException.class, () -> passwordResetService.reset(
                rawToken, "replacement-password", "different-password"
        ));

        assertEquals(1, tokenCount(account.user().id(), false));
    }

    private String createResetToken(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String rawToken = tokenService.generate();
        tokenRepository.revokeActive(userId, AccountActionTokenRepository.PASSWORD_RESET, now);
        tokenRepository.create(
                UUID.randomUUID(), userId, AccountActionTokenRepository.PASSWORD_RESET,
                tokenService.hash(rawToken), now.plusHours(1)
        );
        return rawToken;
    }

    private int tokenCount(UUID userId, boolean includeCompleted) {
        String activeCondition = includeCompleted ? "" : " AND consumed_at IS NULL AND revoked_at IS NULL";
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_action_tokens WHERE user_id = ? AND purpose = ?" + activeCondition,
                Integer.class,
                userId, AccountActionTokenRepository.PASSWORD_RESET
        );
    }

    private String uniqueEmail() {
        return "password-reset-" + UUID.randomUUID() + "@example.com";
    }
}
