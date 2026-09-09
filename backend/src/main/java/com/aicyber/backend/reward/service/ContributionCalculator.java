package com.aicyber.backend.reward.service;

import com.aicyber.backend.reward.model.RewardPolicy;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ContributionCalculator {

    private static final long BASIS_POINTS_DIVISOR = 10_000;

    public long calculate(long orderAmountCents, RewardPolicy policy) {
        if (orderAmountCents <= 0) {
            throw new IllegalArgumentException("orderAmountCents must be positive");
        }
        Objects.requireNonNull(policy, "policy is required");

        long contribution = switch (policy.calculationType()) {
            case "ORDER_TOTAL_PERCENT" -> percentageContribution(orderAmountCents, policy.rateBasisPoints());
            case "FIXED_AMOUNT" -> fixedContribution(policy.fixedAmountCents());
            default -> throw new IllegalArgumentException("Unsupported contribution calculation type");
        };

        if (contribution <= 0) {
            throw new IllegalArgumentException("Contribution must be at least one cent");
        }
        return contribution;
    }

    private long percentageContribution(long orderAmountCents, Integer rateBasisPoints) {
        if (rateBasisPoints == null || rateBasisPoints <= 0 || rateBasisPoints > BASIS_POINTS_DIVISOR) {
            throw new IllegalArgumentException("rateBasisPoints must be between 1 and 10000");
        }
        return Math.multiplyExact(orderAmountCents, rateBasisPoints) / BASIS_POINTS_DIVISOR;
    }

    private long fixedContribution(Long fixedAmountCents) {
        if (fixedAmountCents == null || fixedAmountCents <= 0) {
            throw new IllegalArgumentException("fixedAmountCents must be positive");
        }
        return fixedAmountCents;
    }
}
