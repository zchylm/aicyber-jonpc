package com.aicyber.backend.auth.controller;

import com.aicyber.backend.auth.dto.AuthResponse;
import com.aicyber.backend.auth.dto.AuthMessageResponse;
import com.aicyber.backend.auth.dto.LoginRequest;
import com.aicyber.backend.auth.dto.PasswordResetRequest;
import com.aicyber.backend.auth.dto.RegisterRequest;
import com.aicyber.backend.auth.dto.ResetPasswordRequest;
import com.aicyber.backend.auth.dto.UserResponse;
import com.aicyber.backend.auth.dto.VerifyEmailRequest;
import com.aicyber.backend.auth.service.AuthService;
import com.aicyber.backend.auth.service.EmailVerificationService;
import com.aicyber.backend.auth.service.PasswordResetService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:5174",
        "http://127.0.0.1:5174",
        "https://jonpc.com.au",
        "https://www.jonpc.com.au"
})
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, EmailVerificationService emailVerificationService,
                          PasswordResetService passwordResetService) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return handle(() -> authService.register(request));
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponse login(@RequestBody LoginRequest request) {
        return handle(() -> authService.login(request));
    }

    @PostMapping("/email-verification/send")
    public AuthMessageResponse sendVerification(Authentication authentication) {
        return handle(() -> new AuthMessageResponse(
                emailVerificationService.send(UUID.fromString(authentication.getName()))
        ));
    }

    @PostMapping("/email-verification/confirm")
    public AuthMessageResponse confirmVerification(@RequestBody VerifyEmailRequest request) {
        return handle(() -> {
            emailVerificationService.confirm(request.token());
            return new AuthMessageResponse("Email verified successfully.");
        });
    }

    @PostMapping("/password-reset/request")
    public AuthMessageResponse requestPasswordReset(@RequestBody PasswordResetRequest request) {
        return new AuthMessageResponse(passwordResetService.request(request.email()));
    }

    @PostMapping("/password-reset/confirm")
    public AuthMessageResponse resetPassword(@RequestBody ResetPasswordRequest request) {
        return handle(() -> {
            passwordResetService.reset(request.token(), request.newPassword(), request.confirmPassword());
            return new AuthMessageResponse("Password updated.");
        });
    }

    @GetMapping("/me")
    public UserResponse currentUser(Authentication authentication) {
        try {
            return authService.currentUser(UUID.fromString(authentication.getName()));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token", exception);
        }
    }

    private <T> T handle(RequestOperation<T> operation) {
        try {
            return operation.run();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @FunctionalInterface
    private interface RequestOperation<T> {
        T run();
    }
}
