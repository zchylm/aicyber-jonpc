package com.aicyber.backend.admin.controller;

import com.aicyber.backend.admin.service.CustomBuildReviewService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/custom-build-review")
public class AdminCustomBuildReviewController {
    private final CustomBuildReviewService service;
    public AdminCustomBuildReviewController(CustomBuildReviewService service) { this.service = service; }

    @GetMapping
    public List<CustomBuildReviewService.ReviewItem> list() { return service.list(); }

    @PostMapping("/{requestId}/quotes")
    public CustomBuildReviewService.ReviewItem publish(Authentication authentication, @PathVariable UUID requestId,
                                                        @RequestBody CustomBuildReviewService.PublishQuote request) {
        return service.publish(requestId, UUID.fromString(authentication.getName()), request);
    }
}
