package com.deliveryinsider.simulator.domain.provider.model;

import com.deliveryinsider.simulator.domain.provider.dto.CreateSimulatorOrderRequest;
import com.deliveryinsider.simulator.domain.provider.SimulatorOrderStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder(toBuilder = true)
public record SimulatorOrder(

        com.deliveryinsider.simulator.domain.provider.PlatformType platformType,
        String orderId,
        String createdEventId,
        String storeId,

        long sequence,

        SimulatorOrderStatus status,

        Instant orderedAt,
        Instant eventOccurredAt,

        String deliveryAddress,
        String customerRequest,

        List<CreateSimulatorOrderRequest.Item> items,

        CreateSimulatorOrderRequest.Financials financials,

        String cancelCode,
        String cancelReason

) {

    public SimulatorOrder {
        items = items == null
                ? List.of()
                : List.copyOf(items);
    }
}
