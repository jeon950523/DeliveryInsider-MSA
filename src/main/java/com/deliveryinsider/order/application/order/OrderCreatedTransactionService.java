package com.deliveryinsider.order.application.order;

import com.deliveryinsider.order.application.order.exception.DuplicatePlatformEventException;
import com.deliveryinsider.order.domain.order.entity.OrderEntity;
import com.deliveryinsider.order.domain.order.entity.OrderItemEntity;
import com.deliveryinsider.order.domain.order.entity.OrderProviderChargeEntity;
import com.deliveryinsider.order.domain.order.entity.ProcessedPlatformEvent;
import com.deliveryinsider.order.domain.order.mapper.OrderItemMapper;
import com.deliveryinsider.order.domain.order.mapper.OrderMapper;
import com.deliveryinsider.order.domain.order.mapper.OrderProviderChargeMapper;
import com.deliveryinsider.order.domain.order.mapper.OutboxEventMapper;
import com.deliveryinsider.order.domain.order.mapper.ProcessedPlatformEventMapper;
import com.deliveryinsider.order.domain.order.model.OrderOperationStatus;
import com.deliveryinsider.order.domain.order.model.OrderStatus;
import com.deliveryinsider.order.domain.order.model.ProcessedPlatformEventResult;
import com.deliveryinsider.order.integration.store.dto.StoreOrderSnapshotResponse;
import com.deliveryinsider.order.messaging.platform.dto.PlatformOrderEventData;
import com.deliveryinsider.order.messaging.platform.dto.PlatformOrderEventMessage;
import com.deliveryinsider.order.messaging.platform.exception.NonRetryableOrderEventProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
public class OrderCreatedTransactionService {

    private final ProcessedPlatformEventMapper processedEventMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderProviderChargeMapper chargeMapper;
    private final OutboxEventMapper outboxEventMapper;
    private final OrderOutboxEventFactory outboxFactory;

    @Transactional
    public void create(
        PlatformOrderEventMessage message,
        StoreOrderSnapshotResponse snapshot
    ) {
        insertProcessedEvent(message);

        OrderEntity order =
            createOrder(message);

        insertOrder(order);

        List<OrderItemEntity> items =
            createItems(
                order,
                message,
                snapshot
            );

        items.forEach(
            orderItemMapper::insert
        );

        List<OrderProviderChargeEntity> charges =
            createCharges(
                order,
                message
            );

        charges.forEach(
            chargeMapper::insert
        );

        outboxEventMapper.insert(
            outboxFactory.createOrderCreated(
                order,
                items,
                charges,
                message
            )
        );
    }

    private void insertProcessedEvent(
        PlatformOrderEventMessage message
    ) {
        ProcessedPlatformEvent event =
            ProcessedPlatformEvent.builder()
                .platformType(
                    message.data().platformType()
                )
                .eventId(message.eventId())
                .platformOrderId(
                    message.data().platformOrderId()
                )
                .eventType(message.eventType())
                .sourceSequence(
                    message.data().sourceSequence()
                )
                .processingResult(
                    ProcessedPlatformEventResult.APPLIED
                )
                .build();

        try {
            processedEventMapper.insert(event);

        } catch (DuplicateKeyException e) {
            throw new DuplicatePlatformEventException(
                message.eventId(),
                e
            );
        }
    }

    private OrderEntity createOrder(
        PlatformOrderEventMessage message
    ) {
        PlatformOrderEventData data =
            message.data();

        return OrderEntity.builder()
            .platformType(
                data.platformType()
            )
            .platformOrderId(
                data.platformOrderId()
            )
            .storeId(
                message.storeId()
            )
            .externalStoreId(
                data.externalStoreId()
            )

            /*
             * 외부 플랫폼 상태.
             */
            .status(
                OrderStatus.CREATED
            )

            /*
             * 신규 주문은 점주 입장에서
             * 아직 조리를 시작하지 않은 WAITING.
             */
            .operationStatus(
                OrderOperationStatus.WAITING
            )

            .lastSourceSequence(
                data.sourceSequence()
            )
            .eventVersion(
                1L
            )
            .orderedAt(
                toUtcLocalDateTime(
                    data.orderedAt()
                )
            )
            .providerOccurredAt(
                toUtcLocalDateTime(
                    data.providerOccurredAt()
                )
            )
            .deliveryAddress(
                data.deliveryAddress()
            )
            .customerRequestText(
                data.customerRequestText()
            )
            .providerGrossOrderAmountSnapshot(
                data.grossOrderAmount()
            )
            .providerCustomerPaidAmountSnapshot(
                data.customerPaidAmount()
            )
            .providerMerchantFundedDiscountSnapshot(
                data.merchantFundedDiscount()
            )
            .providerFundedDiscountSnapshot(
                data.providerFundedDiscount()
            )
            .providerFinancialDataStatusSnapshot(
                data.providerFinancialDataStatus()
            )
            .build();
    }

    private void insertOrder(
        OrderEntity order
    ) {
        try {
            orderMapper.insert(order);

        } catch (DuplicateKeyException e) {
            throw new NonRetryableOrderEventProcessingException(
                "PLATFORM_ORDER_ALREADY_EXISTS",
                "이미 동일한 외부 주문이 존재합니다.",
                e
            );
        }
    }

    private List<OrderItemEntity> createItems(
        OrderEntity order,
        PlatformOrderEventMessage message,
        StoreOrderSnapshotResponse snapshot
    ) {
        Map<Long, StoreOrderSnapshotResponse.MenuSnapshot>
            snapshotByMenuId =
            snapshot.menus()
                .stream()
                .collect(
                    toMap(
                        StoreOrderSnapshotResponse
                            .MenuSnapshot::menuId,
                        Function.identity()
                    )
                );

        return message.data()
            .items()
            .stream()
            .map(item ->
                createItem(
                    order.getId(),
                    item,
                    snapshotByMenuId
                )
            )
            .toList();
    }

    private OrderItemEntity createItem(
        Long orderId,
        PlatformOrderEventData.Item item,
        Map<Long, StoreOrderSnapshotResponse.MenuSnapshot>
            snapshotByMenuId
    ) {
        StoreOrderSnapshotResponse.MenuSnapshot snapshot =
            java.util.Optional
                .ofNullable(
                    snapshotByMenuId.get(
                        item.menuId()
                    )
                )
                .orElseThrow(
                    () ->
                        new NonRetryableOrderEventProcessingException(
                            "STORE_SNAPSHOT_MENU_MISSING",
                            "Menu Snapshot을 찾을 수 없습니다."
                        )
                );

        return OrderItemEntity.builder()
            .orderId(
                orderId
            )
            .menuId(
                item.menuId()
            )
            .externalMenuId(
                item.externalMenuId()
            )
            .menuNameSnapshot(
                snapshot.menuName()
            )
            .menuPriceSnapshot(
                snapshot.menuPrice()
            )
            .menuCostSnapshot(
                snapshot.menuCost()
            )
            .packagingCostSnapshot(
                snapshot.packagingCost()
            )
            .expectedCookingTimeSnapshot(
                snapshot.expectedCookingTime()
            )
            .batchCapacitySnapshot(
                snapshot.batchCapacity()
            )
            .orderedUnitPrice(
                item.orderedUnitPrice()
            )
            .quantity(
                item.quantity()
            )
            .build();
    }

    private List<OrderProviderChargeEntity> createCharges(
        OrderEntity order,
        PlatformOrderEventMessage message
    ) {
        return message.data()
            .providerOrderCharges()
            .stream()
            .map(charge ->
                OrderProviderChargeEntity.builder()
                    .orderId(
                        order.getId()
                    )
                    .chargeType(
                        charge.chargeType()
                    )
                    .amount(
                        charge.amount()
                    )
                    .rate(
                        charge.rate()
                    )
                    .basisAmount(
                        charge.basisAmount()
                    )
                    .provisional(
                        charge.provisional()
                    )
                    .sourceCode(
                        charge.sourceCode()
                    )
                    .build()
            )
            .toList();
    }

    private LocalDateTime toUtcLocalDateTime(
        Instant instant
    ) {
        if (instant == null) {
            return null;
        }

        return LocalDateTime.ofInstant(
            instant,
            ZoneOffset.UTC
        );
    }
}
