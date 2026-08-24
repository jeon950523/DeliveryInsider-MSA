package com.deliveryinsider.order.domain.order.mapper;

import com.deliveryinsider.order.domain.order.entity.OrderEntity;
import com.deliveryinsider.order.domain.order.entity.OrderItemEntity;
import com.deliveryinsider.order.domain.order.entity.OrderProviderChargeEntity;
import com.deliveryinsider.order.domain.order.entity.OutboxEventEntity;
import com.deliveryinsider.order.domain.order.entity.ProcessedPlatformEvent;
import com.deliveryinsider.order.domain.order.model.OrderStatus;
import com.deliveryinsider.order.domain.order.model.OutboxStatus;
import com.deliveryinsider.order.domain.order.model.PlatformType;
import com.deliveryinsider.order.domain.order.model.ProcessedPlatformEventResult;
import com.deliveryinsider.order.domain.order.model.ProviderChargeType;
import com.deliveryinsider.order.domain.order.model.ProviderFinancialDataStatus;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@TestConstructor(
    autowireMode = TestConstructor.AutowireMode.ALL
)
@RequiredArgsConstructor
class OrderPersistenceMapperIntegrationTest {

    private final ProcessedPlatformEventMapper processedEventMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderProviderChargeMapper chargeMapper;
    private final OutboxEventMapper outboxEventMapper;

    @Test
    void orderCreatedPersistenceCanBeStoredAndRead() {
        String suffix = UUID.randomUUID().toString();

        String eventId =
            "BAE-EVENT-" + suffix;

        String platformOrderId =
            "BAE-ORDER-" + suffix;

        ProcessedPlatformEvent processedEvent =
            ProcessedPlatformEvent.builder()
                .platformType(PlatformType.BAEMIN)
                .eventId(eventId)
                .platformOrderId(platformOrderId)
                .eventType("ORDER_CREATED")
                .sourceSequence(1L)
                .processingResult(
                    ProcessedPlatformEventResult.APPLIED
                )
                .build();

        int processedInserted =
            processedEventMapper.insert(
                processedEvent
            );

        assertEquals(1, processedInserted);
        assertNotNull(processedEvent.getId());

        OrderEntity order =
            OrderEntity.builder()
                .platformType(PlatformType.BAEMIN)
                .platformOrderId(platformOrderId)
                .storeId(900001L)
                .externalStoreId("BAE-STORE-001")
                .status(OrderStatus.CREATED)
                .lastSourceSequence(1L)
                .eventVersion(1L)
                .orderedAt(
                    LocalDateTime.of(
                        2026,
                        8,
                        24,
                        15,
                        30
                    )
                )
                .providerOccurredAt(
                    LocalDateTime.of(
                        2026,
                        8,
                        24,
                        15,
                        30
                    )
                )
                .deliveryAddress(
                    "대구광역시 동구 테스트 주소"
                )
                .customerRequestText(
                    "문 앞에 놓아주세요."
                )
                .providerGrossOrderAmountSnapshot(
                    18000L
                )
                .providerCustomerPaidAmountSnapshot(
                    17000L
                )
                .providerMerchantFundedDiscountSnapshot(
                    0L
                )
                .providerFundedDiscountSnapshot(
                    1000L
                )
                .providerFinancialDataStatusSnapshot(
                    ProviderFinancialDataStatus.PARTIAL
                )
                .build();

        int orderInserted =
            orderMapper.insert(order);

        assertEquals(1, orderInserted);
        assertNotNull(order.getId());

        OrderItemEntity item =
            OrderItemEntity.builder()
                .orderId(order.getId())
                .menuId(910001L)
                .externalMenuId("BAE-MENU-001")
                .menuNameSnapshot("테스트 메뉴")
                .menuPriceSnapshot(18000L)
                .menuCostSnapshot(7000L)
                .packagingCostSnapshot(500L)
                .orderedUnitPrice(18000L)
                .quantity(1)
                .build();

        int itemInserted =
            orderItemMapper.insert(item);

        assertEquals(1, itemInserted);
        assertNotNull(item.getId());

        OrderProviderChargeEntity charge =
            OrderProviderChargeEntity.builder()
                .orderId(order.getId())
                .chargeType(
                    ProviderChargeType.PLATFORM_ORDER_FEE
                )
                .amount(1000L)
                .rate(new BigDecimal("0.055"))
                .basisAmount(18000L)
                .provisional(true)
                .sourceCode("SIM_PLATFORM_FEE")
                .build();

        int chargeInserted =
            chargeMapper.insert(charge);

        assertEquals(1, chargeInserted);
        assertNotNull(charge.getId());

        OutboxEventEntity outbox =
            OutboxEventEntity.builder()
                .eventId(
                    "ORDER-DOMAIN-EVENT-" + suffix
                )
                .aggregateType("ORDER")
                .aggregateId(
                    order.getId().toString()
                )
                .eventType("ORDER_CREATED")
                .schemaVersion(1)
                .eventVersion(1L)
                .payload(
                    """
                    {
                      "eventType": "ORDER_CREATED",
                      "orderId": %d,
                      "storeId": 900001
                    }
                    """.formatted(order.getId())
                )
                .traceId(null)
                .status(OutboxStatus.PENDING)
                .build();

        int outboxInserted =
            outboxEventMapper.insert(outbox);

        assertEquals(1, outboxInserted);
        assertNotNull(outbox.getId());

        ProcessedPlatformEvent storedEvent =
            processedEventMapper
                .findByPlatformTypeAndEventId(
                    PlatformType.BAEMIN,
                    eventId
                )
                .orElseThrow();

        OrderEntity storedOrder =
            orderMapper
                .findByPlatformIdentity(
                    PlatformType.BAEMIN,
                    platformOrderId
                )
                .orElseThrow();

        List<OrderItemEntity> storedItems =
            orderItemMapper.findAllByOrderId(
                storedOrder.getId()
            );

        List<OrderProviderChargeEntity> storedCharges =
            chargeMapper.findAllByOrderId(
                storedOrder.getId()
            );

        OutboxEventEntity storedOutbox =
            outboxEventMapper
                .findByEventId(
                    outbox.getEventId()
                )
                .orElseThrow();

        assertEquals(
            ProcessedPlatformEventResult.APPLIED,
            storedEvent.getProcessingResult()
        );

        assertEquals(
            OrderStatus.CREATED,
            storedOrder.getStatus()
        );

        assertEquals(
            900001L,
            storedOrder.getStoreId()
        );

        assertEquals(
            ProviderFinancialDataStatus.PARTIAL,
            storedOrder
                .getProviderFinancialDataStatusSnapshot()
        );

        assertEquals(1, storedItems.size());

        assertEquals(
            910001L,
            storedItems.getFirst().getMenuId()
        );

        assertEquals(
            "테스트 메뉴",
            storedItems
                .getFirst()
                .getMenuNameSnapshot()
        );

        assertEquals(1, storedCharges.size());

        assertEquals(
            ProviderChargeType.PLATFORM_ORDER_FEE,
            storedCharges
                .getFirst()
                .getChargeType()
        );

        assertEquals(
            new BigDecimal("0.05500000"),
            storedCharges
                .getFirst()
                .getRate()
        );

        assertEquals(
            OutboxStatus.PENDING,
            storedOutbox.getStatus()
        );

        assertEquals(
            1L,
            storedOutbox.getEventVersion()
        );
    }

    @Test
    void duplicatePlatformEventIsRejected() {
        String eventId =
            "BAE-EVENT-" + UUID.randomUUID();

        ProcessedPlatformEvent first =
            processedEvent(
                eventId,
                "BAE-ORDER-001"
            );

        ProcessedPlatformEvent duplicate =
            processedEvent(
                eventId,
                "BAE-ORDER-001"
            );

        processedEventMapper.insert(first);

        assertThrows(
            DuplicateKeyException.class,
            () ->
                processedEventMapper.insert(
                    duplicate
                )
        );
    }

    @Test
    void duplicatePlatformOrderIdentityIsRejected() {
        String platformOrderId =
            "BAE-ORDER-" + UUID.randomUUID();

        OrderEntity first =
            order(platformOrderId);

        OrderEntity duplicate =
            order(platformOrderId);

        orderMapper.insert(first);

        assertThrows(
            DuplicateKeyException.class,
            () ->
                orderMapper.insert(
                    duplicate
                )
        );
    }

    private ProcessedPlatformEvent processedEvent(
        String eventId,
        String platformOrderId
    ) {
        return ProcessedPlatformEvent.builder()
            .platformType(PlatformType.BAEMIN)
            .eventId(eventId)
            .platformOrderId(platformOrderId)
            .eventType("ORDER_CREATED")
            .sourceSequence(1L)
            .processingResult(
                ProcessedPlatformEventResult.APPLIED
            )
            .build();
    }

    private OrderEntity order(
        String platformOrderId
    ) {
        return OrderEntity.builder()
            .platformType(PlatformType.BAEMIN)
            .platformOrderId(platformOrderId)
            .storeId(900001L)
            .externalStoreId("BAE-STORE-001")
            .status(OrderStatus.CREATED)
            .lastSourceSequence(1L)
            .eventVersion(1L)
            .orderedAt(
                LocalDateTime.now()
            )
            .providerOccurredAt(
                LocalDateTime.now()
            )
            .providerFinancialDataStatusSnapshot(
                ProviderFinancialDataStatus.UNAVAILABLE
            )
            .build();
    }
}
