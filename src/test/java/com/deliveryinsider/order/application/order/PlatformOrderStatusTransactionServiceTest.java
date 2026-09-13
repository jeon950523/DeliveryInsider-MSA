package com.deliveryinsider.order.application.order;

import com.deliveryinsider.order.domain.order.entity.OrderEntity;
import com.deliveryinsider.order.domain.order.entity.OrderItemEntity;
import com.deliveryinsider.order.domain.order.entity.OutboxEventEntity;
import com.deliveryinsider.order.domain.order.mapper.OrderCancellationMapper;
import com.deliveryinsider.order.domain.order.mapper.OrderItemMapper;
import com.deliveryinsider.order.domain.order.mapper.OrderMapper;
import com.deliveryinsider.order.domain.order.mapper.OutboxEventMapper;
import com.deliveryinsider.order.domain.order.mapper.ProcessedPlatformEventMapper;
import com.deliveryinsider.order.domain.order.model.OrderOperationStatus;
import com.deliveryinsider.order.domain.order.model.OrderOperationStatusTransitionPolicy;
import com.deliveryinsider.order.domain.order.model.OrderStatus;
import com.deliveryinsider.order.domain.order.model.OrderStatusTransitionPolicy;
import com.deliveryinsider.order.domain.order.model.PlatformType;
import com.deliveryinsider.order.messaging.platform.dto.PlatformOrderEventData;
import com.deliveryinsider.order.messaging.platform.dto.PlatformOrderEventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformOrderStatusTransactionServiceTest {

    private OrderMapper orderMapper;
    private OrderItemMapper orderItemMapper;
    private ProcessedPlatformEventMapper processedEventMapper;
    private OutboxEventMapper outboxEventMapper;
    private OrderOutboxEventFactory outboxFactory;
    private PlatformOrderStatusTransactionService service;

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        orderItemMapper = mock(OrderItemMapper.class);
        processedEventMapper = mock(ProcessedPlatformEventMapper.class);
        outboxEventMapper = mock(OutboxEventMapper.class);
        outboxFactory = mock(OrderOutboxEventFactory.class);

        service = new PlatformOrderStatusTransactionService(
            orderMapper,
            orderItemMapper,
            mock(OrderCancellationMapper.class),
            processedEventMapper,
            outboxEventMapper,
            outboxFactory,
            new OrderStatusTransitionPolicy(),
            new OrderOperationStatusTransitionPolicy(),
            Clock.fixed(Instant.parse("2026-09-13T08:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void cookingEventAdvancesOnlyOperationStateAndKeepsProviderStatus() {
        OrderEntity order = order(OrderStatus.CREATED, OrderOperationStatus.WAITING, 1L);
        OrderItemEntity item = new OrderItemEntity();
        item.setMenuId(28L);
        when(orderMapper.findByPlatformIdentityForUpdate(PlatformType.BAEMIN, "BAE-ORDER-001"))
            .thenReturn(Optional.of(order));
        when(orderItemMapper.findAllByOrderId(100L)).thenReturn(List.of(item));
        when(orderMapper.updateProviderStatus(
            anyLong(), isNull(), any(), any(), anyLong(), anyLong(),
            any(), any(), any(), any(), any()
        )).thenReturn(1);
        when(outboxFactory.createOrderOperationStatusChanged(any(), any(), any(), any()))
            .thenReturn(mock(OutboxEventEntity.class));

        OrderEventHandlingResult result = service.apply(
            event("COOKING-EVENT", "ORDER_COOKING_STARTED", 2L, "COOKING"),
            new PlatformOrderTransition(null, OrderOperationStatus.COOKING)
        );

        assertEquals(OrderEventHandlingResult.APPLIED, result);
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals(OrderOperationStatus.COOKING, order.getOperationStatus());
        verify(outboxFactory).createOrderOperationStatusChanged(any(), any(), any(), any());
        verify(outboxEventMapper).insert(any());
    }

    @Test
    void createdCannotJumpDirectlyToPickup() {
        OrderEntity order = order(OrderStatus.CREATED, OrderOperationStatus.WAITING, 1L);
        when(orderMapper.findByPlatformIdentityForUpdate(PlatformType.BAEMIN, "BAE-ORDER-001"))
            .thenReturn(Optional.of(order));

        OrderEventHandlingResult result = service.apply(
            event("PICKUP-EVENT", "ORDER_PICKED_UP", 2L, "DELIVERING"),
            new PlatformOrderTransition(OrderStatus.PICKED_UP, OrderOperationStatus.DELIVERING)
        );

        assertEquals(OrderEventHandlingResult.INVALID_TRANSITION_IGNORED, result);
        verify(orderMapper, never()).updateProviderStatus(
            anyLong(), any(), any(), any(), anyLong(), anyLong(),
            any(), any(), any(), any(), any()
        );
        verify(outboxEventMapper, never()).insert(any());
    }

    @Test
    void readyEventAdvancesProviderAndOperationTogether() {
        OrderEntity order = order(OrderStatus.CREATED, OrderOperationStatus.COOKING, 2L);
        when(orderMapper.findByPlatformIdentityForUpdate(PlatformType.BAEMIN, "BAE-ORDER-001"))
            .thenReturn(Optional.of(order));
        when(orderMapper.updateProviderStatus(
            anyLong(), any(), any(), any(), anyLong(), anyLong(),
            any(), any(), any(), any(), any()
        )).thenReturn(1);
        when(outboxFactory.createOrderStatusChanged(any(), any(), any()))
            .thenReturn(mock(OutboxEventEntity.class));

        OrderEventHandlingResult result = service.apply(
            event("READY-EVENT", "ORDER_READY_FOR_PICKUP", 3L, "READY_FOR_PICKUP"),
            new PlatformOrderTransition(OrderStatus.READY_FOR_PICKUP, OrderOperationStatus.READY_FOR_PICKUP)
        );

        assertEquals(OrderEventHandlingResult.APPLIED, result);
        assertEquals(OrderStatus.READY_FOR_PICKUP, order.getStatus());
        assertEquals(OrderOperationStatus.READY_FOR_PICKUP, order.getOperationStatus());
        verify(outboxFactory).createOrderStatusChanged(any(), any(), any());
    }

    @Test
    void unmappedItemBlocksExternalCookingBeforeStateChange() {
        OrderEntity order = order(OrderStatus.CREATED, OrderOperationStatus.WAITING, 1L);
        when(orderMapper.findByPlatformIdentityForUpdate(PlatformType.BAEMIN, "BAE-ORDER-001"))
            .thenReturn(Optional.of(order));
        when(orderItemMapper.findAllByOrderId(100L)).thenReturn(List.of(new OrderItemEntity()));

        org.junit.jupiter.api.Assertions.assertThrows(
            com.deliveryinsider.order.global.error.BusinessException.class,
            () -> service.apply(
                event("COOKING-UNMAPPED", "ORDER_COOKING_STARTED", 2L, "COOKING"),
                new PlatformOrderTransition(null, OrderOperationStatus.COOKING)
            )
        );

        verify(orderMapper, never()).updateProviderStatus(
            anyLong(), any(), any(), any(), anyLong(), anyLong(),
            any(), any(), any(), any(), any()
        );
    }

    @Test
    void staleSequenceCannotMoveCookingOrderBackOrForward() {
        OrderEntity order = order(OrderStatus.CREATED, OrderOperationStatus.COOKING, 3L);
        when(orderMapper.findByPlatformIdentityForUpdate(PlatformType.BAEMIN, "BAE-ORDER-001"))
            .thenReturn(Optional.of(order));

        OrderEventHandlingResult result = service.apply(
            event("STALE-EVENT", "ORDER_READY_FOR_PICKUP", 2L, "READY_FOR_PICKUP"),
            new PlatformOrderTransition(OrderStatus.READY_FOR_PICKUP, OrderOperationStatus.READY_FOR_PICKUP)
        );

        assertEquals(OrderEventHandlingResult.STALE_IGNORED, result);
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals(OrderOperationStatus.COOKING, order.getOperationStatus());
        verify(orderMapper, never()).updateProviderStatus(
            anyLong(), any(), any(), any(), anyLong(), anyLong(),
            any(), any(), any(), any(), any()
        );
    }

    @Test
    void terminalOrderCannotReturnToEarlierState() {
        OrderEntity order = order(OrderStatus.DELIVERED, OrderOperationStatus.COMPLETED, 5L);
        when(orderMapper.findByPlatformIdentityForUpdate(PlatformType.BAEMIN, "BAE-ORDER-001"))
            .thenReturn(Optional.of(order));

        OrderEventHandlingResult result = service.apply(
            event("LATE-READY", "ORDER_READY_FOR_PICKUP", 6L, "READY_FOR_PICKUP"),
            new PlatformOrderTransition(OrderStatus.READY_FOR_PICKUP, OrderOperationStatus.READY_FOR_PICKUP)
        );

        assertEquals(OrderEventHandlingResult.INVALID_TRANSITION_IGNORED, result);
        verify(orderMapper, never()).updateProviderStatus(
            anyLong(), any(), any(), any(), anyLong(), anyLong(),
            any(), any(), any(), any(), any()
        );
    }

    @Test
    void preparedOrderCanStillBeCanceledByManagementFlow() {
        OrderEntity order = order(OrderStatus.READY_FOR_PICKUP, OrderOperationStatus.READY_FOR_PICKUP, 3L);
        when(orderMapper.findByPlatformIdentityForUpdate(PlatformType.BAEMIN, "BAE-ORDER-001"))
            .thenReturn(Optional.of(order));
        when(orderMapper.updateProviderStatus(
            anyLong(), any(), any(), any(), anyLong(), anyLong(),
            any(), any(), any(), any(), any()
        )).thenReturn(1);
        when(outboxFactory.createOrderStatusChanged(any(), any(), any()))
            .thenReturn(mock(OutboxEventEntity.class));

        OrderEventHandlingResult result = service.apply(
            event("CANCEL-EVENT", "ORDER_CANCELED", 4L, "CANCELED"),
            new PlatformOrderTransition(OrderStatus.CANCELED, OrderOperationStatus.CANCELED)
        );

        assertEquals(OrderEventHandlingResult.APPLIED, result);
        assertEquals(OrderStatus.CANCELED, order.getStatus());
        assertEquals(OrderOperationStatus.CANCELED, order.getOperationStatus());
        verify(outboxFactory).createOrderStatusChanged(any(), any(), any());
    }

    private OrderEntity order(
        OrderStatus status,
        OrderOperationStatus operationStatus,
        long sourceSequence
    ) {
        OrderEntity order = OrderEntity.builder()
            .platformType(PlatformType.BAEMIN)
            .platformOrderId("BAE-ORDER-001")
            .storeId(8L)
            .externalStoreId("BAE-STORE-004")
            .status(status)
            .operationStatus(operationStatus)
            .operationVersion(sourceSequence)
            .lastSourceSequence(sourceSequence)
            .eventVersion(sourceSequence)
            .build();
        order.setId(100L);
        return order;
    }

    private PlatformOrderEventMessage event(
        String eventId,
        String eventType,
        long sourceSequence,
        String operationStatus
    ) {
        Instant occurredAt = Instant.parse("2026-09-13T08:00:00Z");
        PlatformOrderEventData data = new PlatformOrderEventData(
            PlatformType.BAEMIN,
            "BAE-ORDER-001",
            "BAE-STORE-004",
            sourceSequence,
            occurredAt,
            occurredAt,
            operationStatus,
            null,
            null,
            List.of(),
            null,
            null,
            null,
            null,
            null,
            List.of(),
            null,
            null
        );
        return new PlatformOrderEventMessage(
            eventId,
            eventType,
            1,
            null,
            occurredAt,
            "trace-test",
            "PLATFORM_ORDER",
            "BAEMIN:BAE-ORDER-001",
            8L,
            data
        );
    }
}
