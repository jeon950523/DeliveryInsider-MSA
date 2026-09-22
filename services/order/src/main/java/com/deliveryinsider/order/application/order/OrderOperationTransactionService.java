package com.deliveryinsider.order.application.order;

import com.deliveryinsider.order.api.order.response.OrderOperationStatusResponse;
import com.deliveryinsider.order.domain.order.entity.OrderEntity;
import com.deliveryinsider.order.domain.order.entity.OrderItemEntity;
import com.deliveryinsider.order.domain.order.entity.OutboxEventEntity;
import com.deliveryinsider.order.domain.order.mapper.OrderItemMapper;
import com.deliveryinsider.order.domain.order.mapper.OrderMapper;
import com.deliveryinsider.order.domain.order.mapper.OutboxEventMapper;
import com.deliveryinsider.order.domain.order.model.OrderOperationStatus;
import com.deliveryinsider.order.domain.order.model.OrderOperationStatusTransitionPolicy;
import com.deliveryinsider.order.global.error.BusinessException;
import com.deliveryinsider.order.global.error.OrderErrorCode;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderOperationTransactionService {

    private final OrderMapper orderMapper;

    private final OrderItemMapper orderItemMapper;

    private final OutboxEventMapper outboxEventMapper;

    private final OrderOutboxEventFactory outboxFactory;

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

        if (targetStatus == OrderOperationStatus.COOKING) {
            requireResolvedMenuMappings(order);
        }

        OrderOperationStatus previousOperationStatus =
            order.getOperationStatus();

        /*
         * DB Timestamp와 Domain Event Timestamp가
         * 동일한 실제 변경 시각을 가리키도록
         * Clock에서 Instant를 한 번만 얻는다.
         */
        Instant operationOccurredAt =
            clock.instant();

        LocalDateTime operationOccurredAtLocal =
            LocalDateTime.ofInstant(
                operationOccurredAt,
                ZoneOffset.UTC
            );

        LocalDateTime cookingStartedAt =
            targetStatus
                == OrderOperationStatus.COOKING
                ? operationOccurredAtLocal
                : null;

        LocalDateTime readyForPickupAt =
            targetStatus
                == OrderOperationStatus.READY_FOR_PICKUP
                ? operationOccurredAtLocal
                : null;

        long nextOperationVersion =
            order.getOperationVersion()
                + 1;

        /*
         * order.events 전체의 순서를 유지한다.
         */
        long nextEventVersion =
            order.getEventVersion()
                + 1;

        int updated =
            orderMapper
                .updateOperationStatus(
                    order.getId(),

                    targetStatus,

                    nextOperationVersion,

                    nextEventVersion,

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

        order.setEventVersion(
            nextEventVersion
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

        /*
         * 상태 UPDATE와 Outbox INSERT가
         * 동일 Transaction에 포함된다.
         *
         * Outbox 저장이 실패하면 상태 변경도 Rollback된다.
         */
        OutboxEventEntity outboxEvent =
            outboxFactory
                .createOrderOperationStatusChanged(
                    order,
                    previousOperationStatus,
                    operationOccurredAt,
                    resolveTraceId()
                );

        outboxEventMapper.insert(
            outboxEvent
        );

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

    private void requireResolvedMenuMappings(OrderEntity order) {
        List<OrderItemEntity> items =
            orderItemMapper.findAllByOrderId(order.getId());

        boolean hasUnresolvedMenu =
            items.isEmpty()
                || items.stream()
                    .anyMatch(item -> item.getMenuId() == null);

        if (hasUnresolvedMenu) {
            throw new BusinessException(
                OrderErrorCode.ORDER_MENU_MAPPING_REQUIRED
            );
        }
    }

    private String resolveTraceId() {
        return Optional
            .ofNullable(
                MDC.get(
                    "traceId"
                )
            )
            .filter(traceId ->
                !traceId.isBlank()
            )
            .orElseGet(() ->
                UUID.randomUUID()
                    .toString()
            );
    }
}
