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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderOperationTransactionServiceTest {

    private OrderMapper orderMapper;
    private OrderItemMapper orderItemMapper;
    private OutboxEventMapper outboxEventMapper;
    private OrderOutboxEventFactory outboxFactory;
    private OrderOperationTransactionService service;

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        orderItemMapper = mock(OrderItemMapper.class);
        outboxEventMapper = mock(OutboxEventMapper.class);
        outboxFactory = mock(OrderOutboxEventFactory.class);

        service = new OrderOperationTransactionService(
            orderMapper,
            orderItemMapper,
            outboxEventMapper,
            outboxFactory,
            new OrderOperationStatusTransitionPolicy(),
            Clock.fixed(
                Instant.parse("2026-09-12T05:00:00Z"),
                ZoneOffset.UTC
            )
        );
    }

    @Test
    void unresolvedMenuPreventsCookingBeforeTheStatusChanges() {
        OrderEntity order = waitingOrder();
        OrderItemEntity unresolvedItem = new OrderItemEntity();

        when(orderMapper.findByIdForUpdate(100L))
            .thenReturn(Optional.of(order));
        when(orderItemMapper.findAllByOrderId(100L))
            .thenReturn(List.of(unresolvedItem));

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> service.change(7L, 100L, OrderOperationStatus.COOKING)
        );

        assertEquals(
            OrderErrorCode.ORDER_MENU_MAPPING_REQUIRED,
            exception.errorCode()
        );
        verify(orderMapper, never()).updateOperationStatus(
            anyLong(), any(), anyLong(), anyLong(),
            any(), any(), any(), any(), any()
        );
        verify(outboxEventMapper, never()).insert(any());
    }

    @Test
    void mappedMenuAllowsTheExistingCookingTransition() {
        OrderEntity order = waitingOrder();
        OrderItemEntity mappedItem = new OrderItemEntity();
        mappedItem.setMenuId(28L);

        when(orderMapper.findByIdForUpdate(100L))
            .thenReturn(Optional.of(order));
        when(orderItemMapper.findAllByOrderId(100L))
            .thenReturn(List.of(mappedItem));
        when(orderMapper.updateOperationStatus(
            anyLong(), any(), anyLong(), anyLong(),
            any(), any(), any(), any(), any()
        )).thenReturn(1);
        when(outboxFactory.createOrderOperationStatusChanged(
            any(), any(), any(), any()
        )).thenReturn(mock(OutboxEventEntity.class));

        OrderOperationStatusResponse response =
            service.change(7L, 100L, OrderOperationStatus.COOKING);

        assertEquals(
            OrderOperationStatus.COOKING,
            response.orderStatus()
        );
        verify(orderMapper).updateOperationStatus(
            anyLong(), any(), anyLong(), anyLong(),
            any(), any(), any(), any(), any()
        );
        verify(outboxEventMapper).insert(any());
    }

    private OrderEntity waitingOrder() {
        OrderEntity order = OrderEntity.builder()
            .storeId(7L)
            .operationStatus(OrderOperationStatus.WAITING)
            .operationVersion(2L)
            .eventVersion(4L)
            .build();
        order.setId(100L);
        return order;
    }
}
