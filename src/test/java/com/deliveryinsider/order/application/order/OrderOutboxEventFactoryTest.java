package com.deliveryinsider.order.application.order;

import com.deliveryinsider.order.domain.order.entity.OrderEntity;
import com.deliveryinsider.order.domain.order.entity.OrderRefundEntity;
import com.deliveryinsider.order.domain.order.model.OrderRefundStatus;
import com.deliveryinsider.order.domain.order.model.PlatformType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderOutboxEventFactoryTest {

    @Test
    void refundRequestUsesSeparateIdempotentAggregateWithoutChangingOrderVersion() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-09-12T06:00:00Z"), ZoneOffset.UTC);
        OrderEntity order = OrderEntity.builder()
            .platformType(PlatformType.BAEMIN)
            .platformOrderId("BAE-ORDER-77")
            .storeId(7L)
            .externalStoreId("BAE-STORE-004")
            .eventVersion(9)
            .build();
        order.setId(77L);
        OrderRefundEntity refund = OrderRefundEntity.builder()
            .orderId(77L)
            .status(OrderRefundStatus.REQUESTED)
            .amount(15000)
            .reasonCode("FOOD_ISSUE")
            .reasonText("음식 문제")
            .requestedAt(LocalDateTime.of(2026, 9, 12, 15, 0))
            .build();

        var outbox = new OrderOutboxEventFactory(JsonMapper.builder().build(), clock)
            .createOrderRefundRequested(order, refund, "trace-77");
        var payload = JsonMapper.builder().build().readTree(outbox.getPayload());

        assertEquals("ORDER_REFUND_REQUESTED", outbox.getEventType());
        assertEquals("ORDER_REFUND", outbox.getAggregateType());
        assertEquals("77", outbox.getAggregateId());
        assertEquals(1, outbox.getEventVersion());
        assertEquals("BAE-STORE-004", payload.path("data").path("externalStoreId").asString());
        assertEquals("REQUESTED", payload.path("data").path("refundStatus").asString());
        assertEquals(15000, payload.path("data").path("amount").asLong());
    }
}
