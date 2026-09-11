package com.deliveryinsider.simulator.domain.baemin.exception;

import com.deliveryinsider.simulator.domain.provider.SimulatorOrderStatus;

public class SimulatorInvalidOrderStatusTransitionException
        extends RuntimeException {

    public SimulatorInvalidOrderStatusTransitionException(
            String orderId,
            SimulatorOrderStatus currentStatus,
            SimulatorOrderStatus targetStatus
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
