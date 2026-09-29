package com.aicyber.backend.auth.service;

import com.aicyber.backend.auth.dto.AuthResponse;
import com.aicyber.backend.auth.dto.LoginRequest;
import com.aicyber.backend.auth.dto.RegisterRequest;
import com.aicyber.backend.auth.dto.UserResponse;
import com.aicyber.backend.auth.model.User;
import com.aicyber.backend.auth.repository.UserRepository;
import com.aicyber.backend.auth.security.JwtService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normaliseEmail(request.email());
        String password = requirePassword(request.password());
        String displayName = requireDisplayName(request.displayName());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException(
                    "An account with this email already exists. Log in or reset your password."
            );
        }
        try {
            OffsetDateTime verifiedAt = emailVerificationService.enabled() ? null : OffsetDateTime.now(ZoneOffset.UTC);
            User user = userRepository.create(
                    UUID.randomUUID(), email, passwordEncoder.encode(password), displayName, verifiedAt
            );
            if (emailVerificationService.enabled()) {
                emailVerificationService.send(user.id());
            }
            return responseFor(user);
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException(
                    "An account with this email already exists. Log in or reset your password.", exception
            );
        }
    }

    public AuthResponse login(LoginRequest request) {
        String email = normaliseEmail(request.email());
        String password = requirePassword(request.password());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email or password is incorrect."));
        if (!"ACTIVE".equals(user.status()) || !passwordEncoder.matches(password, user.passwordHash())) {
            throw new IllegalArgumentException("Email or password is incorrect.");
        }
        return responseFor(user);
    }

    public UserResponse currentUser(UUID userId) {
        return userRepository.findById(userId)
                .map(this::userResponse)
                .orElseThrow(() -> new IllegalArgumentException("User account not found"));
    }

    private AuthResponse responseFor(User user) {
        return new AuthResponse(
                jwtService.createToken(user.id(), user.email(), user.role(), user.authVersion()),
                userResponse(user)
        );
    }

    private UserResponse userResponse(User user) {
        return new UserResponse(user.id(), user.email(), user.displayName(), user.role(), user.emailVerifiedAt() != null);
    }

    private String normaliseEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String requirePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Use at least 8 characters for your password.");
        }
        return password;
    }

    private String requireDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank() || displayName.trim().length() > 120) {
            throw new IllegalArgumentException("Enter a display name of 120 characters or fewer.");
        }
        return displayName.trim();
    }
}
