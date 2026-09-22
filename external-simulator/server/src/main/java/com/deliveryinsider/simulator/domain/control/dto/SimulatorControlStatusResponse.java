package com.deliveryinsider.simulator.domain.control.dto;

import com.deliveryinsider.simulator.domain.provider.PlatformType;

import java.util.List;

public record SimulatorControlStatusResponse(
        boolean demoAutomationAvailable,
        List<ProviderAvailability> providers
) {

    public record ProviderAvailability(
            PlatformType platformType,
            boolean available
    ) {
    }
}
