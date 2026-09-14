package com.deliveryinsider.simulator.domain.baemin.exception;

public class SimulatorInvalidCancellationReasonException extends RuntimeException {

    public SimulatorInvalidCancellationReasonException() {
        super("Cancellation reason is required for a canceled simulator order.");
    }
}
