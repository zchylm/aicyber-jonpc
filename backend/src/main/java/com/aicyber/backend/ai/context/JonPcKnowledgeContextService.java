package com.aicyber.backend.ai.context;

import com.aicyber.backend.catalog.PublicSystemPreview;
import com.aicyber.backend.catalog.SystemCatalogService;
import com.aicyber.backend.payment.config.BankTransferProperties;
import com.aicyber.backend.payment.config.StripeProperties;
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
    private final StripeProperties stripe;
    private final BankTransferProperties bankTransfer;

    public JonPcKnowledgeContextService(SystemCatalogService catalog, RewardQueryService rewards,
                                        StripeProperties stripe, BankTransferProperties bankTransfer) {
        this.catalog = catalog;
        this.rewards = rewards;
        this.stripe = stripe;
        this.bankTransfer = bankTransfer;
    }

    @Override
    public String currentContext() {
        List<PublicSystemPreview> systems = catalog.previews().stream().map(PublicSystemPreview::from).toList();
        StringBuilder context = new StringBuilder("""
                <JON_PC_CONTEXT>
                Source: live public JON.PC Featured Systems catalogue at request time.
                Prices below are AUD and include GST. Availability is public status only; exact inventory quantities are confidential and are not supplied.
                Confirmed warranty for complete JON.PC systems: 3-year parts / 2-year return-to-base. State this label plainly and do not volunteer legal commentary unless the visitor asks about legal rights or remedies.
                """);
        appendWebsiteModules(context);
        appendPaymentMethods(context);
        if (systems.isEmpty()) {
            context.append("No Featured Systems are currently available in the public catalogue.\n");
        } else {
            for (PublicSystemPreview system : systems) appendSystem(context, system);
        }
        appendFounderCampaign(context, rewards.publicSummary());
        return context.append("</JON_PC_CONTEXT>").toString();
    }

    private void appendWebsiteModules(StringBuilder context) {
        context.append("""

                JON.PC website modules and service scope:
                - Complete Systems includes ready-configured desktop PCs for play and work, plus Compact AI Systems for local AI.
                - Desktop and Compact AI products use the same catalogue, stock and shopping-bag flow. A product can be added only when checkout is enabled and stock is available.
                - Saved builds are editable custom configurations. The shopping bag is for stocked finished systems. Orders contain submitted or paid purchases; do not describe these as the same thing.
                - Guided Custom PCs start with customer goals and preferences. Website recommendations are a configurable starting point, not a final price, offer or guarantee of component availability.
                - A submitted Custom PC is reviewed for compatibility, live component availability and supplier pricing. JON.PC sends a final written quote; the customer must accept the current quote and provide delivery or pickup details before checkout.
                - AI Infrastructure lets a business select project scale, compute platform, GPU, CPU, memory, storage, networking, system quantity, physical platform, power, cooling, chassis, operating system, management and optional services.
                - AI Infrastructure options are filtered by explicit platform and compatibility rules. This is not an AI architecture recommendation and does not replace engineering review.
                - A passed website rule means only: Selected hardware checks passed. Infrastructure integration requires engineering review.
                - JON.PC can review selected hardware and project requirements, assemble and test approved compute systems, and integrate and deploy infrastructure within the agreed project scope.
                - Optional infrastructure services can include rack integration, network and cabling, storage and data integration, power and cooling integration, OS and accelerator software, cluster setup, on-site integration, documentation, handover and ongoing support. Service scope and fees are confirmed during engineering review and quotation.
                - Do not describe AI Infrastructure as guaranteed complete data-centre construction or claim that equipment will fit an existing site before engineering review.
                - Customers can submit a completed infrastructure selection for review. An administrator reviews the project and sends a final quote; the customer must accept the current quote before checkout can begin.
                - Melbourne metro delivery is A$25 including GST for eligible postcodes. West Melbourne pickup from 205 Kensington Road is free and by appointment after JON.PC confirms the order is ready.
                - JON.PC builds, tests and dispatches within 7–14 business days after cleared payment unless an accepted quote states otherwise. Carrier transit time starts after dispatch and is not included in that commitment.
                - Dispatched means the parcel has been handed to the carrier with an active tracking number. My Orders shows the recorded status and an Australia Post tracking link when applicable.
                - The 48-hour delivery review period starts from the carrier's recorded Delivered event time, not from dispatch. The customer can confirm receipt or report a problem in My Orders.
                - If the customer does not respond within 48 hours of recorded delivery, receipt is operationally confirmed automatically. This does not limit Australian Consumer Law rights.
                - Complete JON.PC systems include a 3-year parts / 2-year return-to-base warranty. State this label plainly; do not volunteer Australian Consumer Law commentary unless the visitor asks about legal rights or remedies. Do not invent unconfirmed coverage, freight, labour or repair-time details beyond this confirmed label.
                - Product images may be illustrative. A supplied system follows the confirmed specification or a material substitution expressly approved by the customer.
                """);
    }

    private void appendPaymentMethods(StringBuilder context) {
        context.append("\nCurrent checkout methods:\n");
        if (stripe.enabled()) {
            if (stripe.testMode()) {
                context.append("- Stripe Checkout is currently in Test Mode and does not create a real card charge.\n");
            } else if (stripe.liveMode()) {
                context.append("- Stripe Checkout is enabled for real supported online payments. A payment is confirmed only after Stripe reports success.\n");
            } else {
                context.append("- Stripe Checkout availability is not confirmed; direct payment questions to Support.\n");
            }
        } else {
            context.append("- Stripe Checkout is not currently offered.\n");
        }
        if (bankTransfer.enabled()) {
            context.append("- Bank transfer is available. The order is reserved for ")
                    .append(bankTransfer.reservationHours())
                    .append(" hours; customer submission is not proof of payment. The order and its Founder discount are confirmed only after an administrator verifies cleared funds.\n");
        } else {
            context.append("- Bank transfer is not currently offered.\n");
        }
        context.append("- JON.PC does not store full card numbers and never asks for an online-banking password, card PIN or one-time security code.\n");
    }

    private void appendFounderCampaign(StringBuilder context, RewardPublicSummaryResponse campaign) {
        if (!campaign.available()) return;
        context.append("\nFounder 5% Discount campaign:\n")
                .append("- Status: ").append(campaign.programStatus()).append('\n')
                .append("- Eligible paid orders this Melbourne month: ").append(campaign.confirmedCount())
                .append(" of ").append(campaign.maxPositions()).append('\n')
                .append("- Positions remaining: ").append(campaign.remainingPositions()).append('\n');
        if (campaign.currentRateBasisPoints() != null) {
            context.append("- Current offer: ").append(campaign.currentRateBasisPoints() / 100.0)
                    .append("% off the product subtotal; delivery is excluded.\n");
        }
        context.append("- Fifty order places are available each Melbourne calendar month and unused places do not roll over.\n")
                .append("- Each eligible order uses one place; a customer may qualify on multiple separate orders.\n")
                .append("- Stripe holds a place while checkout remains open; bank transfer holds it for the stated 24-hour window. Failed, cancelled or expired payments release the place.\n")
                .append("- The original product price, Founder discount, delivery and final amount are shown before payment. The discount is fully applied at checkout, so no separate claim or payment is required.\n")
                .append("- Cancellation and refund conditions apply; direct policy-specific questions to Support if the supplied context does not confirm the detail.\n");
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
