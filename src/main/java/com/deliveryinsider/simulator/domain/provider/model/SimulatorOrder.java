package com.deliveryinsider.simulator.domain.provider.model;

import com.deliveryinsider.simulator.domain.provider.dto.CreateSimulatorOrderRequest;
import com.deliveryinsider.simulator.domain.provider.SimulatorOrderStatus;
import com.deliveryinsider.simulator.domain.provider.SimulatorOrderOperationStatus;
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

        SimulatorOrderOperationStatus operationStatus,

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

        operationStatus = operationStatus == null
            ? defaultOperationStatus(status)
            : operationStatus;
    }

    public String eventType() {
        if (status == SimulatorOrderStatus.CREATED
            && operationStatus == SimulatorOrderOperationStatus.COOKING) {
            return "ORDER_COOKING_STARTED";
        }
        return status.eventType();
    }

    private static SimulatorOrderOperationStatus defaultOperationStatus(
        SimulatorOrderStatus status
    ) {
        if (status == null) {
            return SimulatorOrderOperationStatus.WAITING;
        }

        return switch (status) {
            case CREATED -> SimulatorOrderOperationStatus.WAITING;
            case READY_FOR_PICKUP -> SimulatorOrderOperationStatus.READY_FOR_PICKUP;
            case PICKED_UP -> SimulatorOrderOperationStatus.DELIVERING;
            case DELIVERED -> SimulatorOrderOperationStatus.COMPLETED;
            case CANCELED -> SimulatorOrderOperationStatus.CANCELED;
        };
    }
}
