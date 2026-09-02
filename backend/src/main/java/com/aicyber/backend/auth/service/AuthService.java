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

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normaliseEmail(request.email());
        String password = requirePassword(request.password());
        String displayName = requireDisplayName(request.displayName());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists");
        }
        try {
            User user = userRepository.create(UUID.randomUUID(), email, passwordEncoder.encode(password), displayName);
            return responseFor(user);
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("An account with this email already exists", exception);
        }
    }

    public AuthResponse login(LoginRequest request) {
        String email = normaliseEmail(request.email());
        String password = requirePassword(request.password());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!"ACTIVE".equals(user.status()) || !passwordEncoder.matches(password, user.passwordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        return responseFor(user);
    }

    public UserResponse currentUser(UUID userId) {
        return userRepository.findById(userId)
                .map(this::userResponse)
                .orElseThrow(() -> new IllegalArgumentException("User account not found"));
    }

    private AuthResponse responseFor(User user) {
        return new AuthResponse(jwtService.createToken(user.id(), user.email(), user.role()), userResponse(user));
    }

    private UserResponse userResponse(User user) {
        return new UserResponse(user.id(), user.email(), user.displayName(), user.role());
    }

    private String normaliseEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("A valid email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String requirePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters");
        }
        return password;
    }

    private String requireDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank() || displayName.trim().length() > 120) {
            throw new IllegalArgumentException("Display name is required and must be at most 120 characters");
        }
        return displayName.trim();
    }
}
