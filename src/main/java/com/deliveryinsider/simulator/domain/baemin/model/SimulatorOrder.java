package com.deliveryinsider.simulator.domain.baemin.model;

import com.deliveryinsider.simulator.domain.baemin.dto.CreateBaeminOrderRequest;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder(toBuilder = true)
public record SimulatorOrder(
    String orderId,
    String createdEventId,
    String storeId,
    long sequence,
    Instant orderedAt,
    Instant eventOccurredAt,
    String deliveryAddress,
    String customerRequest,
    List<CreateBaeminOrderRequest.Item> items,
    CreateBaeminOrderRequest.Financials financials,
    String cancelCode,
    String cancelReason
) {

    public SimulatorOrder {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
