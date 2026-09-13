package com.deliveryinsider.order.domain.order.model;

import org.springframework.stereotype.Component;

@Component
public class OrderOperationStatusTransitionPolicy {

    public boolean isMerchantControllable(
        OrderOperationStatus targetStatus
    ) {
        return false;
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
                    == OrderOperationStatus.COOKING
                    || targetStatus == OrderOperationStatus.CANCELED;

            case COOKING ->
                targetStatus
                    == OrderOperationStatus.READY_FOR_PICKUP
                    || targetStatus == OrderOperationStatus.CANCELED;

            case READY_FOR_PICKUP ->
                targetStatus == OrderOperationStatus.DELIVERING
                    || targetStatus == OrderOperationStatus.CANCELED;

            case DELIVERING ->
                targetStatus == OrderOperationStatus.COMPLETED;

            case COMPLETED,
                 CANCELED ->
                false;
        };
    }
}
