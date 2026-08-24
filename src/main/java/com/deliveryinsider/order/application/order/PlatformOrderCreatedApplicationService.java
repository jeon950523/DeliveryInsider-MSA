package com.deliveryinsider.order.application.order;

import com.deliveryinsider.order.application.order.exception.DuplicatePlatformEventException;
import com.deliveryinsider.order.domain.order.mapper.ProcessedPlatformEventMapper;
import com.deliveryinsider.order.integration.store.StoreOrderSnapshotClient;
import com.deliveryinsider.order.integration.store.dto.StoreOrderSnapshotResponse;
import com.deliveryinsider.order.messaging.platform.dto.PlatformOrderEventMessage;
import com.deliveryinsider.order.messaging.platform.exception.NonRetryableOrderEventProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PlatformOrderCreatedApplicationService {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final ProcessedPlatformEventMapper processedEventMapper;
    private final StoreOrderSnapshotClient snapshotClient;
    private final OrderCreatedTransactionService transactionService;

    public OrderEventHandlingResult handle(
        PlatformOrderEventMessage message
    ) {
        validate(message);

        if (alreadyProcessed(message)) {
            return OrderEventHandlingResult.DUPLICATE_IGNORED;
        }

        List<Long> menuIds =
            message.data()
                .items()
                .stream()
                .map(item -> item.menuId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        StoreOrderSnapshotResponse snapshot =
            snapshotClient.fetch(
                message.storeId(),
                menuIds
            );

        try {
            transactionService.create(
                message,
                snapshot
            );

            return OrderEventHandlingResult.APPLIED;

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
                "Platform 주문 이벤트가 올바르지 않습니다."
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

        if (!"ORDER_CREATED".equals(
            message.eventType()
        )) {
            throw new NonRetryableOrderEventProcessingException(
                "PLATFORM_EVENT_TYPE_UNSUPPORTED",
                "ORDER_CREATED 이벤트가 아닙니다."
            );
        }

        if (
            message.storeId() == null
                || message.data().platformType() == null
                || message.data().platformOrderId() == null
                || message.data().items().isEmpty()
                || message.data()
                .providerFinancialDataStatus() == null
        ) {
            throw new NonRetryableOrderEventProcessingException(
                "PLATFORM_EVENT_REQUIRED_FIELD_MISSING",
                "Platform 주문 이벤트 필수 값이 없습니다."
            );
        }
    }
}
