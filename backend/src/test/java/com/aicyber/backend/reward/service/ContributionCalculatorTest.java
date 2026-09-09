package com.aicyber.backend.reward.service;

import com.aicyber.backend.reward.model.RewardPolicy;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContributionCalculatorTest {

    private final ContributionCalculator calculator = new ContributionCalculator();

    @Test
    void calculatesTwentyFivePercentOfTheOrderInCents() {
        RewardPolicy policy = percentagePolicy(2500);

        assertEquals(50_000, calculator.calculate(200_000, policy));
    }

    @Test
    void roundsFractionalCentsDown() {
        RewardPolicy policy = percentagePolicy(3333);

        assertEquals(33, calculator.calculate(100, policy));
    }

    @Test
    void supportsFixedAmountPolicies() {
        RewardPolicy policy = new RewardPolicy(UUID.randomUUID(), "FIXED_AMOUNT", null, 50_000L);

        assertEquals(50_000, calculator.calculate(200_000, policy));
    }

    @Test
    void rejectsInvalidOrUnsupportedRules() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(200_000, percentagePolicy(0)));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(200_000, new RewardPolicy(UUID.randomUUID(), "PROFIT_PERCENT", 2500, null)));
    }

    private RewardPolicy percentagePolicy(int rateBasisPoints) {
        return new RewardPolicy(UUID.randomUUID(), "ORDER_TOTAL_PERCENT", rateBasisPoints, null);
    }
}
