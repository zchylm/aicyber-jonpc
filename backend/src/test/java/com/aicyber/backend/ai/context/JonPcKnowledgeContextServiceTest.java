package com.aicyber.backend.ai.context;

import com.aicyber.backend.catalog.SystemCatalogService;
import com.aicyber.backend.catalog.SystemProduct;
import com.aicyber.backend.reward.dto.RewardPublicSummaryResponse;
import com.aicyber.backend.reward.service.RewardQueryService;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JonPcKnowledgeContextServiceTest {

    @Test
    void buildsContextFromPublicLiveCatalogueFields() {
        SystemCatalogService catalog = mock(SystemCatalogService.class);
        RewardQueryService rewards = rewards();
        Map<String, String> specifications = new LinkedHashMap<>();
        specifications.put("GPU", "NVIDIA GeForce RTX 5060 Ti");
        specifications.put("CPU", "AMD Ryzen 5 7500F");
        specifications.put("Warranty", "Internal draft warranty");
        when(catalog.previews()).thenReturn(List.of(new SystemProduct(
                UUID.randomUUID(), "JON-STK-5060TI", "JON PC Strike Ti 5060 Ti",
                "A stronger starting point for 1440p gaming.", 239_900,
                8, 8, 1, 0, 2, "Strike/Storm", "PREORDER", "test", null, "1440p High settings",
                "10–15 business days", specifications, "DRAFT", true
        )));

        String context = new JonPcKnowledgeContextService(catalog, rewards).currentContext();

        assertTrue(context.contains("JON PC Strike Ti 5060 Ti"));
        assertTrue(context.contains("$2,399 AUD incl. GST"));
        assertTrue(context.contains("Availability: SOLD OUT"));
        assertTrue(context.contains("GPU: NVIDIA GeForce RTX 5060 Ti"));
        assertTrue(context.contains("Confirmed warranty for the listed systems: 3-year parts / 2-year return-to-base."));
        assertTrue(context.contains("Current cashback: 15.0% of purchase price before GST, capped at A$500"));
        assertTrue(context.contains("does not depend on later customers or future orders"));
        assertFalse(context.contains("0 available"));
        assertFalse(context.contains("Internal draft warranty"));
        assertFalse(context.contains("Flagship"));
    }

    @Test
    void refreshesAvailabilityForEveryQuestion() {
        SystemCatalogService catalog = mock(SystemCatalogService.class);
        RewardQueryService rewards = rewards();
        SystemProduct available = productWithAvailableQuantity(1);
        SystemProduct soldOut = productWithAvailableQuantity(0);
        when(catalog.previews()).thenReturn(List.of(available), List.of(soldOut));
        JonPcKnowledgeContextService knowledge = new JonPcKnowledgeContextService(catalog, rewards);

        String firstQuestionContext = knowledge.currentContext();
        String nextQuestionContext = knowledge.currentContext();

        assertTrue(firstQuestionContext.contains("Availability: AVAILABLE"));
        assertTrue(nextQuestionContext.contains("Availability: SOLD OUT"));
        assertFalse(firstQuestionContext.contains("1 available"));
        assertFalse(nextQuestionContext.contains("0 available"));
        verify(catalog, times(2)).previews();
    }

    private RewardQueryService rewards() {
        RewardQueryService rewards = mock(RewardQueryService.class);
        when(rewards.publicSummary()).thenReturn(new RewardPublicSummaryResponse(
                true, "JON. PC Founders Cashback", "ACTIVE", "AUD", 50, 0, 50,
                "Launch Founder", 1500, 50_000L, 10
        ));
        return rewards;
    }

    private SystemProduct productWithAvailableQuantity(int availableQuantity) {
        return new SystemProduct(
                UUID.randomUUID(), "JON-STM-5070", "JON PC Storm 5070",
                "Built for high-refresh 1440p gaming.", 289_900,
                2, 2, 0, availableQuantity, 1, "Strike/Storm", "PREORDER", "test", null, "1440p Ultra settings",
                "10–15 business days", Map.of("GPU", "NVIDIA GeForce RTX 5070 12GB"),
                "DRAFT", true
        );
    }
}
