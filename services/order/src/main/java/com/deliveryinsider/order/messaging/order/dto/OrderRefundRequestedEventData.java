package com.deliveryinsider.order.messaging.order.dto;

import com.deliveryinsider.order.domain.order.model.OrderRefundStatus;
import com.deliveryinsider.order.domain.order.model.PlatformType;

import java.time.Instant;

public record OrderRefundRequestedEventData(
    Long orderId,
    PlatformType platformType,
    String platformOrderId,
    String externalStoreId,
    OrderRefundStatus refundStatus,
    long amount,
    String reasonCode,
    String reasonText,
    Instant requestedAt
) {
}
