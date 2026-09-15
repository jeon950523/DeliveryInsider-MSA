package com.deliveryinsider.simulator.domain.provider;

public enum SimulatorOrderStatus {

    CREATED("ORDER_CREATED"),
    READY_FOR_PICKUP("ORDER_READY_FOR_PICKUP"),
    PICKED_UP("ORDER_PICKED_UP"),
    DELIVERED("ORDER_DELIVERED"),
    CANCELED("ORDER_CANCELED"),
    REFUND_REQUESTED("ORDER_REFUND_REQUESTED"),
    REFUNDED("ORDER_REFUNDED");

    private final String eventType;

    SimulatorOrderStatus(String eventType) {
        this.eventType = eventType;
    }

    public String eventType() {
        return eventType;
    }

}
