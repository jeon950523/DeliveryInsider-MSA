package com.deliveryinsider.simulator.domain.provider.service;

import com.deliveryinsider.simulator.domain.provider.*;
import com.deliveryinsider.simulator.domain.provider.model.SimulatorOrder;
import com.deliveryinsider.simulator.domain.provider.repository.InMemorySimulatorOrderRepository;
import com.deliveryinsider.simulator.domain.control.repository.SimulatorEventHistoryRepository;
import com.deliveryinsider.simulator.domain.control.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class SimulatorNamespaceTest {
    @Test void equalExternalOrderAndEventIdsAreIndependentAcrossFourProviders() {
        var orders = new InMemorySimulatorOrderRepository();
        var history = new SimulatorEventHistoryRepository();
        for (var provider : PlatformType.values()) {
            var order = SimulatorOrder.builder().platformType(provider).orderId("order-001").storeId(provider.name())
                .createdEventId("event-001").sequence(1).status(SimulatorOrderStatus.CREATED).build();
            orders.save(order, "event-001");
            history.save(new SimulatorEventAttempt("event-001", provider, "ORDER_CREATED", "order-001", SimulatorEventResult.ACCEPTED, 200, Instant.now(), null));
        }
        for (var provider : PlatformType.values()) {
            assertEquals(provider.name(), orders.findEvent(provider, "order-001", "event-001").orElseThrow().storeId());
            assertEquals(provider, history.findLatestBySourceEventId(provider, "event-001").orElseThrow().platformType());
        }
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> history.findLatestBySourceEventId("event-001")).getStatusCode().value());
    }
}
