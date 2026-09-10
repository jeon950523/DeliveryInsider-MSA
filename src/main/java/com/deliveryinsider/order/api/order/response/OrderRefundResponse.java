package com.deliveryinsider.order.api.order.response;

import com.deliveryinsider.order.domain.order.model.OrderRefundStatus;

import java.time.LocalDateTime;

public record OrderRefundResponse(
    Long orderId,
    OrderRefundStatus status,
    long amount,
    String reasonCode,
    LocalDateTime requestedAt,
    String notice
) {
}
