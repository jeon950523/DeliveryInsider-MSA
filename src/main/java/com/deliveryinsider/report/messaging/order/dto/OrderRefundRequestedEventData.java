package com.deliveryinsider.report.messaging.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderRefundRequestedEventData(
    Long orderId,
    String platformType,
    String platformOrderId,
    String externalStoreId,
    String refundStatus,
    long amount,
    String reasonCode,
    String reasonText,
    Instant requestedAt
) {
}
