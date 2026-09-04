package com.deliveryinsider.order.domain.order.model;

import org.springframework.stereotype.Component;

@Component
public class OrderOperationStatusTransitionPolicy {

    public boolean isMerchantControllable(
        OrderOperationStatus targetStatus
    ) {
        return targetStatus
            == OrderOperationStatus.COOKING
            || targetStatus
            == OrderOperationStatus.READY_FOR_PICKUP;
    }

    public boolean canTransition(
        OrderOperationStatus currentStatus,
        OrderOperationStatus targetStatus
    ) {
        if (
            currentStatus == null
                || targetStatus == null
                || currentStatus == targetStatus
        ) {
            return false;
        }

        return switch (currentStatus) {

            case WAITING ->
                targetStatus
                    == OrderOperationStatus.COOKING;

            case COOKING ->
                targetStatus
                    == OrderOperationStatus.READY_FOR_PICKUP;

            case READY_FOR_PICKUP,
                 DELIVERING,
                 COMPLETED,
                 CANCELED ->
                false;
        };
    }
}
