package com.deliveryinsider.simulator.domain.control.exception;

public class SimulatorEventNotFoundException extends RuntimeException {

    public SimulatorEventNotFoundException(String sourceEventId) {
        super("Simulator event not found: " + sourceEventId);
    }
}
