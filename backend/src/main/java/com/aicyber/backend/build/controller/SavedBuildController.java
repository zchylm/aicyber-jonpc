package com.aicyber.backend.build.controller;

import com.aicyber.backend.build.dto.SaveBuildRequest;
import com.aicyber.backend.build.dto.SavedBuildResponse;
import com.aicyber.backend.build.service.SavedBuildService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/builds")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "https://jonpc.com.au", "https://www.jonpc.com.au"})
public class SavedBuildController {

    private final SavedBuildService service;

    public SavedBuildController(SavedBuildService service) {
        this.service = service;
    }

    @GetMapping
    public List<SavedBuildResponse> list(Authentication authentication) {
        return service.list(userId(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SavedBuildResponse save(Authentication authentication, @RequestBody SaveBuildRequest request) {
        try {
            return service.save(userId(authentication), request);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @PutMapping("/{buildId}")
    public SavedBuildResponse update(Authentication authentication, @PathVariable UUID buildId, @RequestBody SaveBuildRequest request) {
        try {
            return service.update(userId(authentication), buildId, request);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @DeleteMapping("/{buildId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable UUID buildId) {
        try {
            service.delete(userId(authentication), buildId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    private UUID userId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login is required");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user identity", exception);
        }
    }
}
