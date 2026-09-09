package com.aicyber.backend.reward.controller;

import com.aicyber.backend.reward.dto.RewardMeResponse;
import com.aicyber.backend.reward.service.RewardQueryService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/rewards")
public class RewardAccountController {
    private final RewardQueryService queryService;

    public RewardAccountController(RewardQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/me")
    public RewardMeResponse me(Authentication authentication) {
        return queryService.memberSummary(UUID.fromString(authentication.getName()));
    }
}
