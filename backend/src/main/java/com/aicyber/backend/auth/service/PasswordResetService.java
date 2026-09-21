package com.aicyber.backend.auth.service;

import com.aicyber.backend.auth.email.AccountEmailSender;
import com.aicyber.backend.auth.repository.AccountActionTokenRepository;
import com.aicyber.backend.auth.repository.UserRepository;
import com.aicyber.backend.auth.security.AccountTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

@Service
public class PasswordResetService {

    public static final String GENERIC_RESPONSE =
            "If the account exists, a reset link is on its way.";
    private static final long REQUEST_COOLDOWN_SECONDS = 60;

    private final UserRepository userRepository;
    private final AccountActionTokenRepository tokenRepository;
    private final AccountTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final AccountEmailSender emailSender;
    private final boolean enabled;
    private final String resetUrl;
    private final long tokenMinutes;

    public PasswordResetService(
            UserRepository userRepository,
            AccountActionTokenRepository tokenRepository,
            AccountTokenService tokenService,
            PasswordEncoder passwordEncoder,
            AccountEmailSender emailSender,
            @Value("${jonpc.auth.password-reset-enabled}") boolean enabled,
            @Value("${jonpc.auth.password-reset-url}") String resetUrl,
            @Value("${jonpc.auth.password-reset-token-minutes}") long tokenMinutes
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.enabled = enabled;
        this.resetUrl = resetUrl;
        this.tokenMinutes = tokenMinutes;
    }

    @Transactional
    public String request(String email) {
        if (!enabled || email == null || email.isBlank()) {
            return GENERIC_RESPONSE;
        }
        var foundUser = userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (foundUser.isEmpty() || !"ACTIVE".equals(foundUser.get().status())) {
            return GENERIC_RESPONSE;
        }

        var user = userRepository.lockById(foundUser.get().id()).orElseThrow();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (tokenRepository.existsCreatedSince(
                user.id(), AccountActionTokenRepository.PASSWORD_RESET,
                now.minusSeconds(REQUEST_COOLDOWN_SECONDS)
        )) {
            return GENERIC_RESPONSE;
        }

        String rawToken = tokenService.generate();
        tokenRepository.revokeActive(user.id(), AccountActionTokenRepository.PASSWORD_RESET, now);
        tokenRepository.create(
                UUID.randomUUID(), user.id(), AccountActionTokenRepository.PASSWORD_RESET,
                tokenService.hash(rawToken), now.plusMinutes(tokenMinutes)
        );
        emailSender.sendPasswordReset(user.email(), user.displayName(), resetLink(rawToken));
        return GENERIC_RESPONSE;
    }

    @Transactional
    public void reset(String rawToken, String newPassword, String confirmPassword) {
        validatePassword(newPassword, confirmPassword);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        var token = tokenRepository.findUsableForUpdate(
                        tokenService.hash(rawToken), AccountActionTokenRepository.PASSWORD_RESET, now
                )
                .orElseThrow(() -> new IllegalArgumentException("This password reset link is invalid or has expired"));
        userRepository.updatePassword(token.userId(), passwordEncoder.encode(newPassword));
        tokenRepository.markConsumed(token.id(), now);
        tokenRepository.revokeActive(token.userId(), AccountActionTokenRepository.PASSWORD_RESET, now);
    }

    private void validatePassword(String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }
    }

    private String resetLink(String rawToken) {
        String separator = resetUrl.contains("?") ? "&" : "?";
        return resetUrl + separator + "resetPasswordToken=" +
                URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }
}
