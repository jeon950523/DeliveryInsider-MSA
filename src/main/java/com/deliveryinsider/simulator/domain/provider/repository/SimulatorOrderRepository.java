package com.deliveryinsider.simulator.domain.provider.repository;

import com.deliveryinsider.simulator.domain.provider.PlatformType;
import com.deliveryinsider.simulator.domain.provider.model.SimulatorOrder;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public interface SimulatorOrderRepository {

    SimulatorOrder save(
        SimulatorOrder order,
        String eventId
    );

    Optional<SimulatorOrder> findById(
        PlatformType provider,
        String orderId
    );

    Optional<SimulatorOrder> findEvent(
        PlatformType provider,
        String orderId,
        String eventId
    );

    List<SimulatorOrder> findRecent(
        PlatformType provider,
        int limit
    );

    SimulatorOrder update(
        PlatformType provider,
        String orderId,
        String eventId,
        UnaryOperator<SimulatorOrder> change
    );
}
