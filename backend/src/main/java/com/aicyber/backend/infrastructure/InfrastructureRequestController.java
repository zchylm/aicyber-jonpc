package com.aicyber.backend.infrastructure;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/infrastructure")
public class InfrastructureRequestController {
    private final InfrastructureRequestService service;

    public InfrastructureRequestController(InfrastructureRequestService service) { this.service = service; }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.OK)
    public InfrastructureRequestService.Response submit(Authentication authentication,
                                                        @RequestBody InfrastructureRequestService.Request request) {
        try {
            return service.submit(UUID.fromString(authentication.getName()), request);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
