package com.aicyber.backend.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminOverviewResponse(
        long totalOrders,
        long pendingPaymentOrders,
        long paidOrders,
        long successfulPaymentCents,
        long waitingRewards,
        long completedRewards,
        long totalAllocatedCents,
        long failedRewardEvents,
        long customReviewsNeeded,
        long featuredSystemsSoldOut,
        long lowStockComponents,
        long inventorySkus,
        long paymentIssues,
        long invoiceEmailIssues,
        long oldestCustomReviewHours,
        long ordersToday,
        long revenueTodayCents,
        long rewardsAllocatedTodayCents,
        long customRequestsToday,
        List<InventoryAlert> inventoryAlerts,
        List<ActivityItem> recentActivity
) {
    public record InventoryAlert(String itemType, String sku, String name, long availableQuantity, long reorderPoint) {}
    public record ActivityItem(String type, String title, String detail, Long amountCents, OffsetDateTime occurredAt) {}
}
