package com.aicyber.backend.ai.context;

import com.aicyber.backend.catalog.SystemCatalogService;
import com.aicyber.backend.catalog.SystemProduct;
import com.aicyber.backend.payment.config.BankTransferProperties;
import com.aicyber.backend.payment.config.StripeProperties;
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
                "10–15 business days", specifications, "DRAFT", true, true
        )));

        String context = new JonPcKnowledgeContextService(catalog, rewards, testStripe(), bankTransfer()).currentContext();

        assertTrue(context.contains("JON PC Strike Ti 5060 Ti"));
        assertTrue(context.contains("$2,399 AUD incl. GST"));
        assertTrue(context.contains("Availability: SOLD OUT"));
        assertTrue(context.contains("GPU: NVIDIA GeForce RTX 5060 Ti"));
        assertTrue(context.contains("3-year parts / 2-year return-to-base"));
        assertTrue(context.contains("do not volunteer legal commentary unless the visitor asks"));
        assertTrue(context.contains("Do not invent unconfirmed coverage, freight, labour or repair-time details"));
        assertTrue(context.contains("Stripe Checkout is currently in Test Mode and does not create a real card charge."));
        assertTrue(context.contains("Bank transfer is available. The order is reserved for 24 hours"));
        assertTrue(context.contains("Melbourne metro delivery is A$25"));
        assertTrue(context.contains("dispatches within 7–14 business days"));
        assertTrue(context.contains("48-hour delivery review period starts from the carrier's recorded Delivered event time"));
        assertTrue(context.contains("Guided Custom PCs start with customer goals and preferences"));
        assertTrue(context.contains("Current offer: 5.0% off the product subtotal"));
        assertTrue(context.contains("Fifty order places are available each Melbourne calendar month"));
        assertTrue(context.contains("There is no later cashback payout"));
        assertTrue(context.contains("Desktop and Compact AI products use the same catalogue"));
        assertTrue(context.contains("Saved builds are editable custom configurations"));
        assertTrue(context.contains("Selected hardware checks passed. Infrastructure integration requires engineering review."));
        assertTrue(context.contains("Service scope and fees are confirmed during engineering review and quotation."));
        assertTrue(context.contains("does not replace engineering review"));
        assertFalse(context.contains("0 available"));
        assertFalse(context.contains("Internal draft warranty"));
        assertFalse(context.contains("two-year return-to-base warranty in addition"));
        assertFalse(context.contains("Real online card payment is not currently available"));
        assertFalse(context.contains("Flagship"));
        assertFalse(context.contains("JON.PC guarantees complete data-centre construction"));
    }

    @Test
    void refreshesAvailabilityForEveryQuestion() {
        SystemCatalogService catalog = mock(SystemCatalogService.class);
        RewardQueryService rewards = rewards();
        SystemProduct available = productWithAvailableQuantity(1);
        SystemProduct soldOut = productWithAvailableQuantity(0);
        when(catalog.previews()).thenReturn(List.of(available), List.of(soldOut));
        JonPcKnowledgeContextService knowledge = new JonPcKnowledgeContextService(catalog, rewards, testStripe(), bankTransfer());

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
                "Monthly Founder Cashback", 500, null, 50
        ));
        return rewards;
    }

    private StripeProperties testStripe() {
        StripeProperties stripe = mock(StripeProperties.class);
        when(stripe.enabled()).thenReturn(true);
        when(stripe.testMode()).thenReturn(true);
        when(stripe.liveMode()).thenReturn(false);
        return stripe;
    }

    private BankTransferProperties bankTransfer() {
        BankTransferProperties bankTransfer = mock(BankTransferProperties.class);
        when(bankTransfer.enabled()).thenReturn(true);
        when(bankTransfer.reservationHours()).thenReturn(24L);
        return bankTransfer;
    }

    private SystemProduct productWithAvailableQuantity(int availableQuantity) {
        return new SystemProduct(
                UUID.randomUUID(), "JON-STM-5070", "JON PC Storm 5070",
                "Built for high-refresh 1440p gaming.", 289_900,
                2, 2, 0, availableQuantity, 1, "Strike/Storm", "PREORDER", "test", null, "1440p Ultra settings",
                "10–15 business days", Map.of("GPU", "NVIDIA GeForce RTX 5070 12GB"),
                "DRAFT", true, true
        );
    }
}
