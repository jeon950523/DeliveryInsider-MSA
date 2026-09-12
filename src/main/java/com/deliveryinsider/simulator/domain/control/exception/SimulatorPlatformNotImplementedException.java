package com.deliveryinsider.simulator.domain.control.exception;

import com.deliveryinsider.simulator.domain.provider.PlatformType;

public class SimulatorPlatformNotImplementedException extends RuntimeException {

    public SimulatorPlatformNotImplementedException(
            PlatformType platformType
    ) {
        super(
                "Simulator provider is not implemented yet: "
                        + platformType
        );
    }
}
