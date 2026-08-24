package com.deliveryinsider.simulator.domain.baemin.repository;

import com.deliveryinsider.simulator.domain.baemin.model.SimulatorOrder;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class SimulatorOrderRepository {

    private final ConcurrentMap<String, SimulatorOrder> orders = new ConcurrentHashMap<>();

    public SimulatorOrder save(SimulatorOrder order) {
        orders.put(order.orderId(), order);
        return order;
    }

    public Optional<SimulatorOrder> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }
}
