package com.deliveryinsider.order.domain.order.model;

import org.springframework.stereotype.Component;

@Component
public class OrderStatusTransitionPolicy {

    public boolean canTransition(
        OrderStatus currentStatus,
        OrderStatus targetStatus
    ) {
        if (
            currentStatus == null
                || targetStatus == null
                || currentStatus == targetStatus
        ) {
            return false;
        }

        return switch (currentStatus) {
            case CREATED ->
                targetStatus == OrderStatus.READY_FOR_PICKUP
                    || targetStatus == OrderStatus.CANCELED;

            case READY_FOR_PICKUP ->
                targetStatus == OrderStatus.PICKED_UP
                    || targetStatus == OrderStatus.CANCELED;

            case PICKED_UP ->
                targetStatus == OrderStatus.DELIVERED;

            case DELIVERED -> targetStatus == OrderStatus.REFUND_REQUESTED;

            case REFUND_REQUESTED -> targetStatus == OrderStatus.REFUNDED;

            case CANCELED, REFUNDED -> false;
        };
    }
}
