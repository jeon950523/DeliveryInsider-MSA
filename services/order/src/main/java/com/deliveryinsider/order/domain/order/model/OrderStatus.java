package com.deliveryinsider.order.domain.order.model;

public enum OrderStatus {

    CREATED,
    READY_FOR_PICKUP,
    PICKED_UP,
    DELIVERED,
    CANCELED,
    REFUND_REQUESTED,
    REFUNDED
}
