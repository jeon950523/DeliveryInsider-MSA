package com.deliveryinsider.order.application.order;

import com.deliveryinsider.order.api.order.response.OrderOperationStatusResponse;
import com.deliveryinsider.order.domain.order.entity.OrderEntity;
import com.deliveryinsider.order.domain.order.mapper.OrderMapper;
import com.deliveryinsider.order.domain.order.model.OrderOperationStatus;
import com.deliveryinsider.order.domain.order.model.OrderOperationStatusTransitionPolicy;
import com.deliveryinsider.order.global.error.BusinessException;
import com.deliveryinsider.order.global.error.OrderErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderOperationTransactionService {

    private final OrderMapper orderMapper;

    private final OrderOperationStatusTransitionPolicy
        transitionPolicy;

    private final Clock clock;

    @Transactional
    public OrderOperationStatusResponse change(
        Long storeId,
        Long orderId,
        OrderOperationStatus targetStatus
    ) {
        OrderEntity order =
            orderMapper
                .findByIdForUpdate(
                    orderId
                )
                .orElseThrow(() ->
                    new BusinessException(
                        OrderErrorCode.ORDER_NOT_FOUND
                    )
                );

        /*
         * 다른 매장의 Order ID를 알아도
         * 존재 여부를 노출하지 않는다.
         */
        if (
            !storeId.equals(
                order.getStoreId()
            )
        ) {
            throw new BusinessException(
                OrderErrorCode.ORDER_NOT_FOUND
            );
        }

        /*
         * DELIVERING / COMPLETED / CANCELED은
         * 점주가 직접 변경하면 안 된다.
         */
        if (
            !transitionPolicy
                .isMerchantControllable(
                    targetStatus
                )
        ) {
            throw new BusinessException(
                OrderErrorCode
                    .OPERATION_STATUS_PROVIDER_CONTROLLED
            );
        }

        if (
            !transitionPolicy
                .canTransition(
                    order.getOperationStatus(),
                    targetStatus
                )
        ) {
            throw new BusinessException(
                OrderErrorCode
                    .INVALID_OPERATION_STATUS_TRANSITION
            );
        }

        LocalDateTime now =
            LocalDateTime.now(
                clock
            );

        LocalDateTime cookingStartedAt =
            targetStatus
                == OrderOperationStatus.COOKING
                ? now
                : null;

        LocalDateTime readyForPickupAt =
            targetStatus
                == OrderOperationStatus.READY_FOR_PICKUP
                ? now
                : null;

        long nextOperationVersion =
            order.getOperationVersion()
                + 1;

        int updated =
            orderMapper
                .updateOperationStatus(
                    order.getId(),
                    targetStatus,
                    nextOperationVersion,
                    cookingStartedAt,
                    readyForPickupAt,
                    null,
                    null,
                    null
                );

        if (updated != 1) {
            throw new IllegalStateException(
                "Order 운영 상태 변경 결과가 1건이 아닙니다. orderId="
                    + order.getId()
            );
        }

        order.setOperationStatus(
            targetStatus
        );

        order.setOperationVersion(
            nextOperationVersion
        );

        if (cookingStartedAt != null) {
            order.setCookingStartedAt(
                cookingStartedAt
            );
        }

        if (readyForPickupAt != null) {
            order.setReadyForPickupAt(
                readyForPickupAt
            );
        }

        return new OrderOperationStatusResponse(
            order.getId(),
            order.getStatus(),
            order.getOperationStatus(),
            order.getOperationVersion(),
            order.getCookingStartedAt(),
            order.getReadyForPickupAt(),
            order.getPickedUpAt(),
            order.getCompletedAt(),
            order.getCanceledAt()
        );
    }
}
