package com.deliveryinsider.platform.domain.provider.order.model;

public enum CanonicalOrderEventType {

    ORDER_CREATED,
    ORDER_COOKING_STARTED,
    ORDER_READY_FOR_PICKUP,
    ORDER_PICKED_UP,
    ORDER_DELIVERED,
    ORDER_CANCELED,
    ORDER_REFUND_REQUESTED,
    ORDER_REFUNDED;

    public static CanonicalOrderEventType from(String value) {
        return CanonicalOrderEventType.valueOf(value);
    }
}
