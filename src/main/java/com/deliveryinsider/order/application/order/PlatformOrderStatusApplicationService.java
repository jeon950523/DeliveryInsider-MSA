package com.deliveryinsider.order.application.order;

import com.deliveryinsider.order.application.order.exception.DuplicatePlatformEventException;
import com.deliveryinsider.order.domain.order.mapper.ProcessedPlatformEventMapper;
import com.deliveryinsider.order.domain.order.model.OrderStatus;
import com.deliveryinsider.order.messaging.platform.dto.PlatformOrderEventMessage;
import com.deliveryinsider.order.messaging.platform.exception.NonRetryableOrderEventProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlatformOrderStatusApplicationService {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final ProcessedPlatformEventMapper processedEventMapper;
    private final PlatformOrderStatusTransactionService transactionService;

    public OrderEventHandlingResult handle(
        PlatformOrderEventMessage message
    ) {
        validate(message);

        if (alreadyProcessed(message)) {
            return OrderEventHandlingResult.DUPLICATE_IGNORED;
        }

        OrderStatus targetStatus =
            resolveTargetStatus(
                message.eventType()
            );

        try {
            return transactionService.apply(
                message,
                targetStatus
            );

        } catch (DuplicatePlatformEventException e) {
            return OrderEventHandlingResult.DUPLICATE_IGNORED;
        }
    }

    private boolean alreadyProcessed(
        PlatformOrderEventMessage message
    ) {
        return processedEventMapper
            .findByPlatformTypeAndEventId(
                message.data().platformType(),
                message.eventId()
            )
            .isPresent();
    }

    private void validate(
        PlatformOrderEventMessage message
    ) {
        if (
            message == null
                || message.data() == null
        ) {
            throw new NonRetryableOrderEventProcessingException(
                "PLATFORM_EVENT_INVALID",
                "Platform 주문 상태 이벤트가 올바르지 않습니다."
            );
        }

        if (
            message.schemaVersion()
                != SUPPORTED_SCHEMA_VERSION
        ) {
            throw new NonRetryableOrderEventProcessingException(
                "PLATFORM_EVENT_SCHEMA_UNSUPPORTED",
                "지원하지 않는 Platform Event Schema입니다."
            );
        }

        if (
            message.data().platformType() == null
                || message.data().platformOrderId() == null
        ) {
            throw new NonRetryableOrderEventProcessingException(
                "PLATFORM_EVENT_REQUIRED_FIELD_MISSING",
                "Platform 주문 상태 이벤트 필수 값이 없습니다."
            );
        }

        resolveTargetStatus(
            message.eventType()
        );
    }

    private OrderStatus resolveTargetStatus(
        String eventType
    ) {
        if (eventType == null) {
            throw unsupportedEventType();
        }

        return switch (eventType) {
            case "ORDER_PICKED_UP" ->
                OrderStatus.PICKED_UP;

            case "ORDER_DELIVERED" ->
                OrderStatus.DELIVERED;

            case "ORDER_CANCELED" ->
                OrderStatus.CANCELED;

            default ->
                throw unsupportedEventType();
        };
    }

    private NonRetryableOrderEventProcessingException
    unsupportedEventType() {

        return new NonRetryableOrderEventProcessingException(
            "PLATFORM_EVENT_TYPE_UNSUPPORTED",
            "지원하지 않는 Platform 주문 상태 이벤트입니다."
        );
    }
}
