package com.deliveryinsider.simulator.domain.provider.dto;

import com.deliveryinsider.simulator.domain.provider.SimulatorOrderStatus;
import com.deliveryinsider.simulator.domain.provider.SimulatorOrderOperationStatus;

public record ChangeSimulatorOrderStatusRequest(

        SimulatorOrderStatus status,

        SimulatorOrderOperationStatus operationStatus,

        String cancelCode,

        String cancelReason

) {
}
