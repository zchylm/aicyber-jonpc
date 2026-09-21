package com.aicyber.backend.reward.controller;

import com.aicyber.backend.reward.dto.RewardDemoResetRequest;
import com.aicyber.backend.reward.dto.RewardDemoResponse;
import com.aicyber.backend.reward.service.RewardDemoService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/rewards/demo")
@Profile("local")
@ConditionalOnProperty(name = "jonpc.rewards.demo-enabled", havingValue = "true")
public class RewardDemoController {
    private final RewardDemoService demoService;

    public RewardDemoController(RewardDemoService demoService) {
        this.demoService = demoService;
    }

    @PostMapping("/qualify-latest-request")
    public RewardDemoResponse qualifyLatestRequest(Authentication authentication) {
        return execute(() -> demoService.qualifyLatestBuildRequest(userId(authentication)));
    }

    @PostMapping("/create-founder-order")
    public RewardDemoResponse createFounderOrder(Authentication authentication) {
        return execute(() -> demoService.createFounderOrder(userId(authentication)));
    }

    @PostMapping("/make-cashback-payable")
    public RewardDemoResponse makeCashbackPayable(Authentication authentication) {
        return execute(() -> demoService.makeLatestCashbackPayable(userId(authentication)));
    }

    @PostMapping("/mark-cashback-paid")
    public RewardDemoResponse markCashbackPaid(Authentication authentication) {
        return execute(() -> demoService.markLatestCashbackPaid(userId(authentication)));
    }

    @PostMapping("/reset")
    public RewardDemoResponse reset(
            Authentication authentication,
            @RequestBody RewardDemoResetRequest request
    ) {
        return execute(() -> demoService.reset(userId(authentication), request.confirmation()));
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    private RewardDemoResponse execute(DemoAction action) {
        try {
            return action.run();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @FunctionalInterface
    private interface DemoAction {
        RewardDemoResponse run();
    }
}
