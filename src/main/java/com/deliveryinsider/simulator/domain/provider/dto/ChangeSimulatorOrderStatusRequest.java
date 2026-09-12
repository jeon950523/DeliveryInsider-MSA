package com.deliveryinsider.simulator.domain.provider.dto;

import com.deliveryinsider.simulator.domain.provider.SimulatorOrderStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeSimulatorOrderStatusRequest(

        @NotNull
        SimulatorOrderStatus status,

        String cancelCode,

        String cancelReason

) {
}
