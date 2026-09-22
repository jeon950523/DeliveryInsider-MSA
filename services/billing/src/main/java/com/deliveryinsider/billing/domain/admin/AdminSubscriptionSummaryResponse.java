package com.deliveryinsider.billing.domain.admin;

public record AdminSubscriptionSummaryResponse(
    long totalSubscriptionCount,
    long activeSubscriptionCount,
    long pastDueSubscriptionCount,
    long canceledSubscriptionCount
) {
}
