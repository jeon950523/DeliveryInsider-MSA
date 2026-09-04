package com.deliveryinsider.order.messaging.order.dto;

import com.deliveryinsider.order.domain.order.model.OrderOperationStatus;
import com.deliveryinsider.order.domain.order.model.PlatformType;

import java.time.Instant;

public record OrderOperationStatusChangedEventData(

    Long orderId,

    PlatformType platformType,
    String platformOrderId,
    String externalStoreId,

    OrderOperationStatus previousOperationStatus,
    OrderOperationStatus operationStatus,

    long operationVersion,

    Instant operationOccurredAt

) {
}
