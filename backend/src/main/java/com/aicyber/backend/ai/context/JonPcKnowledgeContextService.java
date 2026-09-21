package com.aicyber.backend.ai.context;

import com.aicyber.backend.catalog.PublicSystemPreview;
import com.aicyber.backend.catalog.SystemCatalogService;
import com.aicyber.backend.reward.dto.RewardPublicSummaryResponse;
import com.aicyber.backend.reward.service.RewardQueryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class JonPcKnowledgeContextService implements KnowledgeContextProvider {

    private static final List<String> SPECIFICATION_ORDER = List.of(
            "CPU", "GPU", "Memory", "Storage", "Motherboard", "PSU", "Cooler",
            "Case", "Fans", "Networking", "Accessories", "OS"
    );

    private final SystemCatalogService catalog;
    private final RewardQueryService rewards;

    public JonPcKnowledgeContextService(SystemCatalogService catalog, RewardQueryService rewards) {
        this.catalog = catalog;
        this.rewards = rewards;
    }

    @Override
    public String currentContext() {
        List<PublicSystemPreview> systems = catalog.previews().stream().map(PublicSystemPreview::from).toList();
        StringBuilder context = new StringBuilder("""
                <JON_PC_CONTEXT>
                Source: live public JON.PC Featured Systems catalogue at request time.
                Prices below are AUD and include GST. Availability is public status only; exact inventory quantities are confidential and are not supplied.
                Confirmed warranty for the listed systems: 3-year parts / 2-year return-to-base.
                """);
        if (systems.isEmpty()) {
            context.append("No Featured Systems are currently available in the public catalogue.\n");
        } else {
            for (PublicSystemPreview system : systems) appendSystem(context, system);
        }
        appendFounderCampaign(context, rewards.publicSummary());
        return context.append("</JON_PC_CONTEXT>").toString();
    }

    private void appendFounderCampaign(StringBuilder context, RewardPublicSummaryResponse campaign) {
        if (!campaign.available()) return;
        context.append("\nFounder Cashback campaign:\n")
                .append("- Status: ").append(campaign.programStatus()).append('\n')
                .append("- Confirmed Founder positions: ").append(campaign.confirmedCount())
                .append(" of ").append(campaign.maxPositions()).append('\n')
                .append("- Positions remaining: ").append(campaign.remainingPositions()).append('\n');
        if (campaign.currentRateBasisPoints() != null && campaign.currentCapCents() != null) {
            context.append("- Current tier: ").append(campaign.currentTierName()).append('\n')
                    .append("- Current cashback: ").append(campaign.currentRateBasisPoints() / 100.0)
                    .append("% of purchase price before GST, capped at A$")
                    .append(campaign.currentCapCents() / 100).append('\n');
        }
        context.append("- A Founder position and exact cashback amount are locked only after successful payment.\n")
                .append("- A locked cashback amount does not depend on later customers or future orders.\n")
                .append("- The pilot is limited to 50 Founder positions and one Founder reward per purchaser.\n")
                .append("- JON.PC initiates eligible cashback to the original payment method within 30 days after confirmed delivery. The payment provider may require additional processing time.\n")
                .append("- Cancellation, refund, validation and payout conditions apply; direct policy-specific questions to Support if the supplied context does not confirm the detail.\n");
    }

    private void appendSystem(StringBuilder context, PublicSystemPreview system) {
        context.append("\nSystem: ").append(system.name()).append('\n')
                .append("SKU: ").append(system.sku()).append('\n');
        if (system.description() != null && !system.description().isBlank()) {
            context.append("Description: ").append(system.description()).append('\n');
        }
        context
                .append("Price: ").append(String.format(Locale.ROOT, "$%,d AUD incl. GST", system.priceCents() / 100)).append('\n')
                .append("Availability: ").append(system.available() ? "AVAILABLE" : "SOLD OUT").append('\n')
                .append("Recommended for: ").append(system.recommendedFor()).append('\n')
                .append("Estimated dispatch: ").append(system.dispatchEstimate()).append('\n')
                .append("Confirmed specifications:\n");

        for (Map.Entry<String, String> specification : orderedSpecifications(system.specifications())) {
            context.append("- ").append(specification.getKey()).append(": ").append(specification.getValue()).append('\n');
        }
    }

    private List<Map.Entry<String, String>> orderedSpecifications(Map<String, String> specifications) {
        List<Map.Entry<String, String>> ordered = new ArrayList<>();
        for (String key : SPECIFICATION_ORDER) {
            if (specifications.containsKey(key)) ordered.add(Map.entry(key, specifications.get(key)));
        }
        specifications.entrySet().stream()
                .filter(entry -> !SPECIFICATION_ORDER.contains(entry.getKey()))
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(ordered::add);
        return ordered;
    }
}
