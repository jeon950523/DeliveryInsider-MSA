package com.deliveryinsider.order.application.order;

import com.deliveryinsider.order.domain.order.model.OrderOperationStatus;
import com.deliveryinsider.order.domain.order.model.OrderStatus;

record PlatformOrderTransition(
    OrderStatus providerStatus,
    OrderOperationStatus operationStatus
) {
}
