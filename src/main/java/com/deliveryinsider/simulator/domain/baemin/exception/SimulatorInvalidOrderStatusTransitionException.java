package com.deliveryinsider.simulator.domain.baemin.exception;

public class SimulatorInvalidOrderStatusTransitionException
        extends RuntimeException {

    public SimulatorInvalidOrderStatusTransitionException(
            String orderId,
            Object currentStatus,
            Object targetStatus
    ) {
        super(
                "Invalid simulator order status transition. "
                        + "orderId=%s, current=%s, target=%s"
                        .formatted(
                                orderId,
                                currentStatus,
                                targetStatus
                        )
        );
    }
}
