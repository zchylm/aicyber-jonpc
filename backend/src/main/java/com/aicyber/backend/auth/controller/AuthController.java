package com.aicyber.backend.auth.controller;

import com.aicyber.backend.auth.dto.AuthResponse;
import com.aicyber.backend.auth.dto.LoginRequest;
import com.aicyber.backend.auth.dto.RegisterRequest;
import com.aicyber.backend.auth.dto.UserResponse;
import com.aicyber.backend.auth.service.AuthService;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
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
