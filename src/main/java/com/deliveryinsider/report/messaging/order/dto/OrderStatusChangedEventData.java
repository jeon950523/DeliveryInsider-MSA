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

    String operationStatus,

    Long sourceSequence,

    Instant providerOccurredAt,

    String providerCancelCode,
    String providerCancelReason,
    String providerRefundId,
    Long providerRefundAmount,
    String providerRefundReasonCode,
    String providerRefundReason

) {

    public OrderStatusChangedEventData(
        Long orderId, String platformType, String platformOrderId, String externalStoreId,
        String previousStatus, String status, String operationStatus, Long sourceSequence,
        Instant providerOccurredAt, String providerCancelCode, String providerCancelReason
    ) {
        this(orderId, platformType, platformOrderId, externalStoreId, previousStatus, status, operationStatus,
            sourceSequence, providerOccurredAt, providerCancelCode, providerCancelReason, null, null, null, null);
    }
}
