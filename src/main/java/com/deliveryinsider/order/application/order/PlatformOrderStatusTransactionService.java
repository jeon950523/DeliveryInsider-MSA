package com.deliveryinsider.order.application.order;

import com.deliveryinsider.order.application.order.exception.DuplicatePlatformEventException;
import com.deliveryinsider.order.domain.order.entity.OrderEntity;
import com.deliveryinsider.order.domain.order.entity.OutboxEventEntity;
import com.deliveryinsider.order.domain.order.entity.ProcessedPlatformEvent;
import com.deliveryinsider.order.domain.order.mapper.OrderMapper;
import com.deliveryinsider.order.domain.order.mapper.OutboxEventMapper;
import com.deliveryinsider.order.domain.order.mapper.ProcessedPlatformEventMapper;
import com.deliveryinsider.order.domain.order.model.OrderOperationStatus;
import com.deliveryinsider.order.domain.order.model.OrderStatus;
import com.deliveryinsider.order.domain.order.model.OrderStatusTransitionPolicy;
import com.deliveryinsider.order.domain.order.model.ProcessedPlatformEventResult;
import com.deliveryinsider.order.messaging.platform.dto.PlatformOrderEventMessage;
import com.deliveryinsider.order.messaging.platform.exception.RetryableOrderEventProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class PlatformOrderStatusTransactionService {

    private final OrderMapper orderMapper;
    private final ProcessedPlatformEventMapper processedEventMapper;
    private final OutboxEventMapper outboxEventMapper;
    private final OrderOutboxEventFactory outboxFactory;
    private final OrderStatusTransitionPolicy transitionPolicy;
    private final Clock clock;

    @Transactional
    public OrderEventHandlingResult apply(
        PlatformOrderEventMessage message,
        OrderStatus targetStatus
    ) {
        OrderEntity order =
            orderMapper
                .findByPlatformIdentityForUpdate(
                    message.data().platformType(),
                    message.data().platformOrderId()
                )
                .orElseThrow(() ->
                    new RetryableOrderEventProcessingException(
                        "ORDER_NOT_FOUND_FOR_STATUS_EVENT",
                        "상태를 변경할 주문을 찾을 수 없습니다.",
                        null
                    )
                );

        OrderEventHandlingResult result =
            determineResult(
                order,
                message,
                targetStatus
            );

        insertProcessedEvent(
            message,
            toProcessingResult(result)
        );

        if (result != OrderEventHandlingResult.APPLIED) {
            return result;
        }

        OrderStatus previousStatus =
            order.getStatus();

        OrderOperationStatus targetOperationStatus =
            resolveTargetOperationStatus(
                targetStatus
            );

        long nextEventVersion =
            order.getEventVersion() + 1;

        long nextOperationVersion =
            order.getOperationVersion() + 1;

        LocalDateTime providerOccurredAt =
            resolveProviderOccurredAt(
                message.data().providerOccurredAt()
            );

        LocalDateTime pickedUpAt =
            targetStatus == OrderStatus.PICKED_UP
                ? providerOccurredAt
                : null;

        LocalDateTime completedAt =
            targetStatus == OrderStatus.DELIVERED
                ? providerOccurredAt
                : null;

        LocalDateTime canceledAt =
            targetStatus == OrderStatus.CANCELED
                ? providerOccurredAt
                : null;

        int updated =
            orderMapper.updateProviderStatus(
                order.getId(),
                targetStatus,
                targetOperationStatus,
                message.data().sourceSequence(),
                nextEventVersion,
                nextOperationVersion,
                pickedUpAt,
                completedAt,
                canceledAt
            );

        if (updated != 1) {
            throw new IllegalStateException(
                "Order 상태 변경 결과가 1건이 아닙니다. orderId="
                    + order.getId()
            );
        }

        applyUpdatedState(
            order,
            targetStatus,
            targetOperationStatus,
            message.data().sourceSequence(),
            nextEventVersion,
            nextOperationVersion,
            pickedUpAt,
            completedAt,
            canceledAt
        );

        OutboxEventEntity outboxEvent =
            outboxFactory
                .createOrderStatusChanged(
                    order,
                    previousStatus,
                    message
                );

        outboxEventMapper.insert(
            outboxEvent
        );

        return OrderEventHandlingResult.APPLIED;
    }

    private OrderEventHandlingResult determineResult(
        OrderEntity order,
        PlatformOrderEventMessage message,
        OrderStatus targetStatus
    ) {
        Long incomingSequence =
            message.data().sourceSequence();

        Long lastSequence =
            order.getLastSourceSequence();

        if (
            incomingSequence != null
                && lastSequence != null
                && incomingSequence <= lastSequence
        ) {
            return OrderEventHandlingResult
                .STALE_IGNORED;
        }

        if (!transitionPolicy.canTransition(
            order.getStatus(),
            targetStatus
        )) {
            return OrderEventHandlingResult
                .INVALID_TRANSITION_IGNORED;
        }

        return OrderEventHandlingResult.APPLIED;
    }

    private OrderOperationStatus resolveTargetOperationStatus(
        OrderStatus targetStatus
    ) {
        return switch (targetStatus) {
            case PICKED_UP ->
                OrderOperationStatus.DELIVERING;

            case DELIVERED ->
                OrderOperationStatus.COMPLETED;

            case CANCELED ->
                OrderOperationStatus.CANCELED;

            case CREATED ->
                throw new IllegalArgumentException(
                    "CREATED는 Platform 상태변경 대상이 아닙니다."
                );
        };
    }

    private LocalDateTime resolveProviderOccurredAt(
        Instant providerOccurredAt
    ) {
        Instant occurredAt =
            providerOccurredAt != null
                ? providerOccurredAt
                : clock.instant();

        return LocalDateTime.ofInstant(
            occurredAt,
            ZoneOffset.UTC
        );
    }

    private void applyUpdatedState(
        OrderEntity order,
        OrderStatus targetStatus,
        OrderOperationStatus targetOperationStatus,
        Long sourceSequence,
        long eventVersion,
        long operationVersion,
        LocalDateTime pickedUpAt,
        LocalDateTime completedAt,
        LocalDateTime canceledAt
    ) {
        order.setStatus(
            targetStatus
        );

        order.setOperationStatus(
            targetOperationStatus
        );

        order.setEventVersion(
            eventVersion
        );

        order.setOperationVersion(
            operationVersion
        );

        if (sourceSequence != null) {
            order.setLastSourceSequence(
                sourceSequence
            );
        }

        if (pickedUpAt != null) {
            order.setPickedUpAt(
                pickedUpAt
            );
        }

        if (completedAt != null) {
            order.setCompletedAt(
                completedAt
            );
        }

        if (canceledAt != null) {
            order.setCanceledAt(
                canceledAt
            );
        }
    }

    private ProcessedPlatformEventResult
    toProcessingResult(
        OrderEventHandlingResult result
    ) {
        return switch (result) {
            case APPLIED ->
                ProcessedPlatformEventResult.APPLIED;

            case STALE_IGNORED ->
                ProcessedPlatformEventResult.STALE_IGNORED;

            case INVALID_TRANSITION_IGNORED ->
                ProcessedPlatformEventResult
                    .INVALID_TRANSITION_IGNORED;

            case DUPLICATE_IGNORED ->
                throw new IllegalArgumentException(
                    "중복 이벤트는 신규 처리 이력으로 기록하지 않습니다."
                );
        };
    }

    private void insertProcessedEvent(
        PlatformOrderEventMessage message,
        ProcessedPlatformEventResult result
    ) {
        ProcessedPlatformEvent event =
            ProcessedPlatformEvent.builder()
                .platformType(
                    message.data().platformType()
                )
                .eventId(
                    message.eventId()
                )
                .platformOrderId(
                    message.data().platformOrderId()
                )
                .eventType(
                    message.eventType()
                )
                .sourceSequence(
                    message.data().sourceSequence()
                )
                .processingResult(
                    result
                )
                .build();

        try {
            processedEventMapper.insert(
                event
            );

        } catch (DuplicateKeyException e) {
            throw new DuplicatePlatformEventException(
                message.eventId(),
                e
            );
        }
    }
}
