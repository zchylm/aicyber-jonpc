package com.aicyber.backend.reward.controller;

import com.aicyber.backend.reward.dto.RewardPublicSummaryResponse;
import com.aicyber.backend.reward.service.RewardQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rewards")
public class RewardPublicController {
    private final RewardQueryService queryService;

    public RewardPublicController(RewardQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/summary")
    public RewardPublicSummaryResponse summary() {
        return queryService.publicSummary();
    }
}
