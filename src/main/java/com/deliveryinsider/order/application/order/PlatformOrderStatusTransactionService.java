package com.deliveryinsider.order.application.order;

import com.deliveryinsider.order.application.order.exception.DuplicatePlatformEventException;
import com.deliveryinsider.order.domain.order.entity.OrderEntity;
import com.deliveryinsider.order.domain.order.entity.OutboxEventEntity;
import com.deliveryinsider.order.domain.order.entity.ProcessedPlatformEvent;
import com.deliveryinsider.order.domain.order.mapper.OrderMapper;
import com.deliveryinsider.order.domain.order.mapper.OutboxEventMapper;
import com.deliveryinsider.order.domain.order.mapper.ProcessedPlatformEventMapper;
import com.deliveryinsider.order.domain.order.model.OrderStatus;
import com.deliveryinsider.order.domain.order.model.OrderStatusTransitionPolicy;
import com.deliveryinsider.order.domain.order.model.ProcessedPlatformEventResult;
import com.deliveryinsider.order.messaging.platform.dto.PlatformOrderEventMessage;
import com.deliveryinsider.order.messaging.platform.exception.RetryableOrderEventProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformOrderStatusTransactionService {

    private final OrderMapper orderMapper;
    private final ProcessedPlatformEventMapper processedEventMapper;
    private final OutboxEventMapper outboxEventMapper;
    private final OrderOutboxEventFactory outboxFactory;
    private final OrderStatusTransitionPolicy transitionPolicy;

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

        if (
            result
                != OrderEventHandlingResult.APPLIED
        ) {
            return result;
        }

        OrderStatus previousStatus =
            order.getStatus();

        long nextEventVersion =
            order.getEventVersion() + 1;

        int updated =
            orderMapper.updateStatus(
                order.getId(),
                targetStatus,
                message.data().sourceSequence(),
                nextEventVersion
            );

        if (updated != 1) {
            throw new IllegalStateException(
                "Order 상태 변경 결과가 1건이 아닙니다. orderId="
                    + order.getId()
            );
        }

        order.setStatus(targetStatus);
        order.setEventVersion(
            nextEventVersion
        );

        if (
            message.data().sourceSequence()
                != null
        ) {
            order.setLastSourceSequence(
                message.data().sourceSequence()
            );
        }

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
