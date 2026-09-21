package com.aicyber.backend.auth.service;

import com.aicyber.backend.auth.email.AccountEmailSender;
import com.aicyber.backend.auth.model.User;
import com.aicyber.backend.auth.repository.AccountActionTokenRepository;
import com.aicyber.backend.auth.repository.UserRepository;
import com.aicyber.backend.auth.security.AccountTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final AccountActionTokenRepository tokenRepository;
    private final AccountTokenService tokenService;
    private final AccountEmailSender emailSender;
    private final boolean enabled;
    private final String verificationUrl;
    private final long tokenHours;

    public EmailVerificationService(
            UserRepository userRepository,
            AccountActionTokenRepository tokenRepository,
            AccountTokenService tokenService,
            AccountEmailSender emailSender,
            @Value("${jonpc.auth.email-verification-enabled}") boolean enabled,
            @Value("${jonpc.auth.email-verification-url}") String verificationUrl,
            @Value("${jonpc.auth.email-verification-token-hours}") long tokenHours
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.tokenService = tokenService;
        this.emailSender = emailSender;
        this.enabled = enabled;
        this.verificationUrl = verificationUrl;
        this.tokenHours = tokenHours;
    }

    public boolean enabled() {
        return enabled;
    }

    @Transactional
    public String send(UUID userId) {
        if (!enabled) {
            return "Email verification is not enabled in this environment.";
        }
        User user = userRepository.lockById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User account not found"));
        if (user.emailVerifiedAt() != null) {
            return "Your email is already verified.";
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String rawToken = tokenService.generate();
        tokenRepository.revokeActive(user.id(), AccountActionTokenRepository.EMAIL_VERIFICATION, now);
        tokenRepository.create(
                UUID.randomUUID(),
                user.id(),
                AccountActionTokenRepository.EMAIL_VERIFICATION,
                tokenService.hash(rawToken),
                now.plusHours(tokenHours)
        );
        emailSender.sendVerification(user.email(), user.displayName(), verificationLink(rawToken));
        return "Verification email sent.";
    }

    @Transactional
    public void confirm(String rawToken) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        var token = tokenRepository.findUsableForUpdate(
                        tokenService.hash(rawToken),
                        AccountActionTokenRepository.EMAIL_VERIFICATION,
                        now
                )
                .orElseThrow(() -> new IllegalArgumentException("This verification link is invalid or has expired"));
        tokenRepository.markConsumed(token.id(), now);
        userRepository.markEmailVerified(token.userId(), now);
        tokenRepository.revokeActive(token.userId(), AccountActionTokenRepository.EMAIL_VERIFICATION, now);
    }

    private String verificationLink(String rawToken) {
        String separator = verificationUrl.contains("?") ? "&" : "?";
        return verificationUrl + separator + "verifyEmailToken=" +
                URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }
}
