package com.deliveryinsider.order.application.order;

import com.deliveryinsider.order.domain.order.entity.OrderEntity;
import com.deliveryinsider.order.domain.order.entity.OrderItemEntity;
import com.deliveryinsider.order.domain.order.entity.OrderProviderChargeEntity;
import com.deliveryinsider.order.domain.order.entity.ProcessedPlatformEvent;
import com.deliveryinsider.order.domain.order.mapper.OrderItemMapper;
import com.deliveryinsider.order.domain.order.mapper.OrderMapper;
import com.deliveryinsider.order.domain.order.mapper.OrderProviderChargeMapper;
import com.deliveryinsider.order.domain.order.mapper.ProcessedPlatformEventMapper;
import com.deliveryinsider.order.domain.order.model.OrderStatus;
import com.deliveryinsider.order.domain.order.model.OutboxStatus;
import com.deliveryinsider.order.domain.order.model.PlatformType;
import com.deliveryinsider.order.domain.order.model.ProcessedPlatformEventResult;
import com.deliveryinsider.order.domain.order.model.ProviderChargeType;
import com.deliveryinsider.order.domain.order.model.ProviderFinancialDataStatus;
import com.deliveryinsider.order.integration.store.dto.StoreOrderSnapshotResponse;
import com.deliveryinsider.order.messaging.platform.dto.PlatformOrderEventData;
import com.deliveryinsider.order.messaging.platform.dto.PlatformOrderEventMessage;
import com.deliveryinsider.order.messaging.platform.exception.NonRetryableOrderEventProcessingException;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestConstructor(
    autowireMode = TestConstructor.AutowireMode.ALL
)
@RequiredArgsConstructor
class OrderCreatedTransactionServiceIntegrationTest {

    private final OrderCreatedTransactionService transactionService;

    private final ProcessedPlatformEventMapper processedEventMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderProviderChargeMapper chargeMapper;

    private final JdbcTemplate jdbcTemplate;

    private String eventId;
    private String platformOrderId;

    @AfterEach
    void cleanUp() {
        if (platformOrderId == null) {
            return;
        }

        orderMapper
            .findByPlatformIdentity(
                PlatformType.BAEMIN,
                platformOrderId
            )
            .ifPresent(order -> {
                jdbcTemplate.update(
                    """
                    DELETE FROM outbox_events
                    WHERE aggregate_type = 'ORDER'
                      AND aggregate_id = ?
                    """,
                    order.getId().toString()
                );

                jdbcTemplate.update(
                    """
                    DELETE FROM order_provider_charges
                    WHERE order_id = ?
                    """,
                    order.getId()
                );

                jdbcTemplate.update(
                    """
                    DELETE FROM order_items
                    WHERE order_id = ?
                    """,
                    order.getId()
                );

                jdbcTemplate.update(
                    """
                    DELETE FROM orders
                    WHERE id = ?
                    """,
                    order.getId()
                );
            });

        if (eventId != null) {
            jdbcTemplate.update(
                """
                DELETE FROM processed_platform_events
                WHERE platform_type = 'BAEMIN'
                  AND event_id = ?
                """,
                eventId
            );
        }
    }

    @Test
    void orderCreatedIsPersistedWithSnapshotChargeAndOutbox() {
        PlatformOrderEventMessage message =
            message();

        StoreOrderSnapshotResponse snapshot =
            completeSnapshot();

        transactionService.create(
            message,
            snapshot
        );

        ProcessedPlatformEvent processed =
            processedEventMapper
                .findByPlatformTypeAndEventId(
                    PlatformType.BAEMIN,
                    eventId
                )
                .orElseThrow();

        OrderEntity order =
            orderMapper
                .findByPlatformIdentity(
                    PlatformType.BAEMIN,
                    platformOrderId
                )
                .orElseThrow();

        List<OrderItemEntity> items =
            orderItemMapper
                .findAllByOrderId(
                    order.getId()
                );

        List<OrderProviderChargeEntity> charges =
            chargeMapper
                .findAllByOrderId(
                    order.getId()
                );

        Integer outboxCount =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM outbox_events
                WHERE aggregate_type = 'ORDER'
                  AND aggregate_id = ?
                  AND event_type = 'ORDER_CREATED'
                """,
                Integer.class,
                order.getId().toString()
            );

        String outboxStatus =
            jdbcTemplate.queryForObject(
                """
                SELECT status
                FROM outbox_events
                WHERE aggregate_type = 'ORDER'
                  AND aggregate_id = ?
                LIMIT 1
                """,
                String.class,
                order.getId().toString()
            );

        assertEquals(
            ProcessedPlatformEventResult.APPLIED,
            processed.getProcessingResult()
        );

        assertEquals(
            OrderStatus.CREATED,
            order.getStatus()
        );

        assertEquals(
            900001L,
            order.getStoreId()
        );

        assertEquals(
            "BAE-STORE-001",
            order.getExternalStoreId()
        );

        assertEquals(
            1L,
            order.getLastSourceSequence()
        );

        assertEquals(
            1L,
            order.getEventVersion()
        );

        assertEquals(
            ProviderFinancialDataStatus.PARTIAL,
            order.getProviderFinancialDataStatusSnapshot()
        );

        assertEquals(1, items.size());

        OrderItemEntity item =
            items.getFirst();

        assertEquals(
            910001L,
            item.getMenuId()
        );

        assertEquals(
            "BAE-MENU-001",
            item.getExternalMenuId()
        );

        assertEquals(
            "후라이드치킨",
            item.getMenuNameSnapshot()
        );

        assertEquals(
            18000L,
            item.getMenuPriceSnapshot()
        );

        assertEquals(
            7000L,
            item.getMenuCostSnapshot()
        );

        assertEquals(
            500L,
            item.getPackagingCostSnapshot()
        );

        assertEquals(
            18000L,
            item.getOrderedUnitPrice()
        );

        assertEquals(1, charges.size());

        OrderProviderChargeEntity charge =
            charges.getFirst();

        assertEquals(
            ProviderChargeType.PLATFORM_ORDER_FEE,
            charge.getChargeType()
        );

        assertEquals(
            1000L,
            charge.getAmount()
        );

        assertEquals(
            0,
            charge.getRate()
                .compareTo(
                    new BigDecimal("0.055")
                )
        );

        assertEquals(1, outboxCount);

        assertEquals(
            OutboxStatus.PENDING.name(),
            outboxStatus
        );
    }

    @Test
    void transactionRollsBackWhenMenuSnapshotIsMissing() {
        PlatformOrderEventMessage message =
            message();

        StoreOrderSnapshotResponse invalidSnapshot =
            new StoreOrderSnapshotResponse(
                900001L,
                List.of()
            );

        assertThrows(
            NonRetryableOrderEventProcessingException.class,
            () ->
                transactionService.create(
                    message,
                    invalidSnapshot
                )
        );

        assertTrue(
            processedEventMapper
                .findByPlatformTypeAndEventId(
                    PlatformType.BAEMIN,
                    eventId
                )
                .isEmpty()
        );

        assertTrue(
            orderMapper
                .findByPlatformIdentity(
                    PlatformType.BAEMIN,
                    platformOrderId
                )
                .isEmpty()
        );

        Integer itemCount =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM order_items oi
                JOIN orders o
                  ON o.id = oi.order_id
                WHERE o.platform_type = 'BAEMIN'
                  AND o.platform_order_id = ?
                """,
                Integer.class,
                platformOrderId
            );

        Integer outboxCount =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM outbox_events
                WHERE payload LIKE ?
                """,
                Integer.class,
                "%" + platformOrderId + "%"
            );

        assertEquals(0, itemCount);
        assertEquals(0, outboxCount);
    }

    private PlatformOrderEventMessage message() {
        String suffix =
            UUID.randomUUID().toString();

        eventId =
            "BAE-EVENT-" + suffix;

        platformOrderId =
            "BAE-ORDER-" + suffix;

        PlatformOrderEventData data =
            new PlatformOrderEventData(
                PlatformType.BAEMIN,
                platformOrderId,
                "BAE-STORE-001",
                1L,
                Instant.parse(
                    "2026-08-24T06:00:00Z"
                ),
                Instant.parse(
                    "2026-08-24T06:01:00Z"
                ),
                "대구광역시 동구 테스트 주소",
                "문 앞에 놓아주세요.",
                List.of(
                    new PlatformOrderEventData.Item(
                        910001L,
                        "BAE-MENU-001",
                        1,
                        18000L
                    )
                ),
                ProviderFinancialDataStatus.PARTIAL,
                18000L,
                17000L,
                0L,
                1000L,
                List.of(
                    new PlatformOrderEventData.Charge(
                        ProviderChargeType.PLATFORM_ORDER_FEE,
                        1000L,
                        new BigDecimal("0.055"),
                        18000L,
                        true,
                        "SIM_PLATFORM_FEE"
                    )
                ),
                null,
                null
            );

        return new PlatformOrderEventMessage(
            eventId,
            "ORDER_CREATED",
            1,
            null,
            Instant.parse(
                "2026-08-24T06:01:00Z"
            ),
            "trace-order-created-test",
            "PLATFORM_ORDER",
            "BAEMIN:" + platformOrderId,
            900001L,
            data
        );
    }

    private StoreOrderSnapshotResponse completeSnapshot() {
        return new StoreOrderSnapshotResponse(
            900001L,
            List.of(
                new StoreOrderSnapshotResponse.MenuSnapshot(
                    910001L,
                    "후라이드치킨",
                    18000L,
                    7000L,
                    500L,
                    15,
                    3
                )
            )
        );
    }
}
