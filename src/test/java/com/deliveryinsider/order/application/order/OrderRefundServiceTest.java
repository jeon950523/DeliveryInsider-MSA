package com.deliveryinsider.order.application.order;

import com.deliveryinsider.order.api.order.request.CreateOrderRefundRequest;
import com.deliveryinsider.order.domain.order.entity.OrderEntity;
import com.deliveryinsider.order.domain.order.entity.OrderRefundEntity;
import com.deliveryinsider.order.domain.order.entity.OutboxEventEntity;
import com.deliveryinsider.order.domain.order.mapper.OrderItemMapper;
import com.deliveryinsider.order.domain.order.mapper.OrderMapper;
import com.deliveryinsider.order.domain.order.mapper.OrderRefundMapper;
import com.deliveryinsider.order.domain.order.mapper.OutboxEventMapper;
import com.deliveryinsider.order.domain.order.model.CancellationReasonCode;
import com.deliveryinsider.order.domain.order.model.OrderStatus;
import com.deliveryinsider.order.integration.store.CurrentStoreClient;
import com.deliveryinsider.order.integration.store.dto.CurrentStoreResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderRefundServiceTest {

    @AfterEach
    void clearTraceId() {
        MDC.remove("traceId");
    }

    @Test
    void refundOutboxRetainsRequestTraceId() {
        CurrentStoreClient currentStoreClient = mock(CurrentStoreClient.class);
        OrderMapper orderMapper = mock(OrderMapper.class);
        OrderItemMapper itemMapper = mock(OrderItemMapper.class);
        OrderRefundMapper refundMapper = mock(OrderRefundMapper.class);
        OutboxEventMapper outboxEventMapper = mock(OutboxEventMapper.class);
        OrderOutboxEventFactory outboxEventFactory =
            mock(OrderOutboxEventFactory.class);
        Clock clock = Clock.fixed(
            Instant.parse("2026-09-12T10:30:00Z"),
            ZoneOffset.UTC
        );
        OrderEntity order = OrderEntity.builder()
            .storeId(7L)
            .status(OrderStatus.DELIVERED)
            .providerGrossOrderAmountSnapshot(18_000L)
            .build();
        order.setId(80L);
        OutboxEventEntity outbox = mock(OutboxEventEntity.class);

        when(currentStoreClient.findByUserId(27L)).thenReturn(
            new CurrentStoreResponse(7L, "집", "09:00", "09:00")
        );
        when(orderMapper.findByIdForUpdate(80L)).thenReturn(
            Optional.of(order)
        );
        when(refundMapper.findByOrderId(80L)).thenReturn(Optional.empty());
        when(outboxEventFactory.createOrderRefundRequested(
            eq(order),
            any(OrderRefundEntity.class),
            eq("audit-trace-80")
        )).thenReturn(outbox);
        MDC.put("traceId", "audit-trace-80");

        new OrderRefundService(
            currentStoreClient,
            orderMapper,
            itemMapper,
            refundMapper,
            outboxEventMapper,
            outboxEventFactory,
            clock
        ).request(
            27L,
            80L,
            new CreateOrderRefundRequest(
                CancellationReasonCode.MERCHANT_REQUEST,
                "감사 환불 요청"
            )
        );

        verify(refundMapper).insert(any(OrderRefundEntity.class));
        verify(outboxEventMapper).insert(outbox);
        verify(outboxEventFactory).createOrderRefundRequested(
            eq(order),
            any(OrderRefundEntity.class),
            eq("audit-trace-80")
        );
    }
}
