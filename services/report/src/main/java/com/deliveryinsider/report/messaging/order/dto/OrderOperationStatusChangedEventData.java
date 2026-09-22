package com.deliveryinsider.report.messaging.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderOperationStatusChangedEventData(

    Long orderId,

    String platformType,
    String platformOrderId,
    String externalStoreId,

    String previousOperationStatus,
    String operationStatus,

    long operationVersion,

    Instant operationOccurredAt

) {
}
