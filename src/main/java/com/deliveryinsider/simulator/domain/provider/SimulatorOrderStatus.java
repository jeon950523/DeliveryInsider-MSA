package com.deliveryinsider.simulator.domain.provider;

public enum SimulatorOrderStatus {

    CREATED("ORDER_CREATED"),
    /**
     * The Order service owns this merchant-readiness transition. Platform
     * synchronizes it to the simulator so only provider delivery actions may
     * follow; it must not be sent back as a provider webhook.
     */
    READY_FOR_PICKUP("ORDER_READY_FOR_PICKUP"),
    PICKED_UP("ORDER_PICKED_UP"),
    DELIVERED("ORDER_DELIVERED"),
    CANCELED("ORDER_CANCELED");

    private final String eventType;

    SimulatorOrderStatus(String eventType) {
        this.eventType = eventType;
    }

    public String eventType() {
        return eventType;
    }

    public boolean canTransitionTo(
            SimulatorOrderStatus target
    ) {
        if (target == null || this == target) {
            return false;
        }

        return switch (this) {
            case CREATED ->
                    target == READY_FOR_PICKUP
                            || target == CANCELED;

            case READY_FOR_PICKUP ->
                    target == PICKED_UP;

            case PICKED_UP ->
                    target == DELIVERED;

            case DELIVERED, CANCELED ->
                    false;
        };
    }
}
