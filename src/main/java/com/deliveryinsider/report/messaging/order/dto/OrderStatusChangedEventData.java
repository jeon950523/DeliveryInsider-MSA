package com.deliveryinsider.report.messaging.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderStatusChangedEventData(
    Long orderId,
    String platformType,
    String platformOrderId,
    String externalStoreId,

    String previousStatus,
    String status,

    Long sourceSequence,
    Instant providerOccurredAt,

    String providerCancelCode,
    String providerCancelReason
) {
}
