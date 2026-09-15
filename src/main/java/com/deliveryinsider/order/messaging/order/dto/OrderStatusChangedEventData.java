package com.deliveryinsider.order.messaging.order.dto;

import com.deliveryinsider.order.domain.order.model.OrderOperationStatus;
import com.deliveryinsider.order.domain.order.model.OrderStatus;
import com.deliveryinsider.order.domain.order.model.PlatformType;

import java.time.Instant;

public record OrderStatusChangedEventData(

    Long orderId,

    PlatformType platformType,
    String platformOrderId,
    String externalStoreId,

    OrderStatus previousStatus,
    OrderStatus status,

    OrderOperationStatus operationStatus,

    Long sourceSequence,
    Instant providerOccurredAt,

    String providerCancelCode,
    String providerCancelReason,
    String providerRefundId,
    Long providerRefundAmount,
    String providerRefundReasonCode,
    String providerRefundReason

) {
}
