package com.deliveryinsider.order.api.order.response;

import com.deliveryinsider.order.domain.order.model.OrderOperationStatus;
import com.deliveryinsider.order.domain.order.model.OrderStatus;

import java.time.LocalDateTime;

public record OrderOperationStatusResponse(

    Long orderId,

    OrderStatus providerStatus,

    OrderOperationStatus orderStatus,

    long operationVersion,

    LocalDateTime cookingStartedAt,

    LocalDateTime readyForPickupAt,

    LocalDateTime pickedUpAt,

    LocalDateTime completedAt,

    LocalDateTime canceledAt

) {
}
