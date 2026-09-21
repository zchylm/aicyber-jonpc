package com.aicyber.backend.order.controller;

import com.aicyber.backend.admin.service.CustomBuildReviewService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/quotes")
public class CustomQuoteController {
    private final CustomBuildReviewService service;
    public CustomQuoteController(CustomBuildReviewService service) { this.service = service; }

    @PostMapping("/{quoteId}/accept")
    public void accept(Authentication authentication, @PathVariable UUID quoteId) {
        service.accept(quoteId, UUID.fromString(authentication.getName()));
    }
}
