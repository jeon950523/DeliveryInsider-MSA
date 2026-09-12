package com.deliveryinsider.simulator.domain.control.dto;

import com.deliveryinsider.simulator.domain.provider.PlatformType;
import com.deliveryinsider.simulator.domain.provider.SimulatorOrderStatus;

import java.time.Instant;
import java.util.List;

public record SimulatorControlOrderResponse(
    PlatformType platformType,
    String externalOrderId,
    String externalStoreId,
    SimulatorOrderStatus status,
    long sequence,
    Instant orderedAt,
    Instant eventOccurredAt,
    String deliveryAddress,
    String customerRequest,
    long totalAmount,
    List<Item> items,
    String cancelCode,
    String cancelReason
) {

    public record Item(
        String externalMenuId,
        int quantity,
        long unitPrice
    ) {
    }
}
