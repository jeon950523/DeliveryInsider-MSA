package com.deliveryinsider.simulator.domain.baemin.exception;

public class SimulatorOrderNotFoundException extends RuntimeException {

    public SimulatorOrderNotFoundException(String orderId) {
        super("Simulator order not found: " + orderId);
    }
}
