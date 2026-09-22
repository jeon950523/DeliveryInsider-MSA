package com.deliveryinsider.order.domain.order.admin;

public record AdminOrderSummaryResponse(
    long todayOrderCount,
    long activeOrderCount,
    long completedOrderCount,
    long canceledOrderCount
) {
}
