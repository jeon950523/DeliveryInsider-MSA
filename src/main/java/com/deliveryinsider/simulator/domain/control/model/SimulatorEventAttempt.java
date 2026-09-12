package com.deliveryinsider.simulator.domain.control.model;

import com.deliveryinsider.simulator.domain.provider.PlatformType;

import java.time.Instant;

public record SimulatorEventAttempt(
        String sourceEventId,
        PlatformType platformType,
        String eventType,
        String externalOrderId,
        SimulatorEventResult result,
        Integer httpStatus,
        Instant sentAt,
        String errorMessage
) {
}
