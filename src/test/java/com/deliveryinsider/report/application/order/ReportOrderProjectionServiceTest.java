package com.deliveryinsider.report.application.order;

import com.deliveryinsider.report.domain.order.entity.ReportOrderEntity;
import com.deliveryinsider.report.domain.order.entity.ReportRefundEntity;
import com.deliveryinsider.report.domain.order.mapper.ReportCancellationMapper;
import com.deliveryinsider.report.domain.order.mapper.ReportOrderChargeMapper;
import com.deliveryinsider.report.domain.order.mapper.ReportOrderItemMapper;
import com.deliveryinsider.report.domain.order.mapper.ReportOrderMapper;
import com.deliveryinsider.report.domain.order.mapper.ReportRefundMapper;
import com.deliveryinsider.report.messaging.order.dto.OrderEventEnvelope;
import com.deliveryinsider.report.messaging.order.dto.OrderRefundRequestedEventData;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportOrderProjectionServiceTest {

    @Test
    void refundRequestIsProjectedSeparatelyAndDuplicateIsStale() {
        ReportOrderMapper orders = mock(ReportOrderMapper.class);
        ReportRefundMapper refunds = mock(ReportRefundMapper.class);
        JsonMapper jsonMapper = JsonMapper.builder().build();
        ReportOrderProjectionService service = new ReportOrderProjectionService(
            orders,
            mock(ReportOrderItemMapper.class),
            mock(ReportOrderChargeMapper.class),
            mock(ReportCancellationMapper.class),
            refunds,
            jsonMapper
        );
        ReportOrderEntity order = ReportOrderEntity.builder()
            .orderId(77L)
            .storeId(7L)
            .platformType("BAEMIN")
            .platformOrderId("BAE-ORDER-77")
            .externalStoreId("BAE-STORE-004")
            .build();
        when(orders.findByOrderIdForUpdate(77L)).thenReturn(Optional.of(order));
        when(refunds.insert(org.mockito.ArgumentMatchers.any()))
            .thenReturn(1)
            .thenThrow(new org.springframework.dao.DuplicateKeyException("duplicate"));

        OrderRefundRequestedEventData data = new OrderRefundRequestedEventData(
            77L,
            "BAEMIN",
            "BAE-ORDER-77",
            "BAE-STORE-004",
            "REQUESTED",
            15000,
            "FOOD_ISSUE",
            "음식 문제",
            Instant.parse("2026-09-12T06:00:00Z")
        );
        OrderEventEnvelope event = new OrderEventEnvelope(
            "event-77",
            "ORDER_REFUND_REQUESTED",
            1,
            1,
            Instant.parse("2026-09-12T06:00:00Z"),
            "trace-77",
            "ORDER_REFUND",
            "77",
            7L,
            jsonMapper.valueToTree(data)
        );

        assertEquals(ReportProjectionResult.APPLIED, service.handle(event));
        assertEquals(ReportProjectionResult.STALE_IGNORED, service.handle(event));

        ArgumentCaptor<ReportRefundEntity> refund = ArgumentCaptor.forClass(ReportRefundEntity.class);
        verify(refunds, org.mockito.Mockito.times(2)).insert(refund.capture());
        ReportRefundEntity firstRefund = refund.getAllValues().get(0);
        assertEquals("REQUESTED", firstRefund.getStatus());
        assertEquals(15000, firstRefund.getAmount());
        assertEquals("FOOD_ISSUE", firstRefund.getReasonCode());
        assertEquals("음식 문제", firstRefund.getReasonText());
    }

    @Test
    void malformedRefundEnvelopeIsRejectedBeforeProjectionWrite() {
        ReportOrderMapper orders = mock(ReportOrderMapper.class);
        ReportRefundMapper refunds = mock(ReportRefundMapper.class);
        JsonMapper jsonMapper = JsonMapper.builder().build();
        ReportOrderProjectionService service = new ReportOrderProjectionService(
            orders,
            mock(ReportOrderItemMapper.class),
            mock(ReportOrderChargeMapper.class),
            mock(ReportCancellationMapper.class),
            refunds,
            jsonMapper
        );
        OrderRefundRequestedEventData data = new OrderRefundRequestedEventData(
            77L, "BAEMIN", "BAE-ORDER-77", "BAE-STORE-004", "COMPLETED",
            15000, "FOOD_ISSUE", "음식 문제", Instant.parse("2026-09-12T06:00:00Z")
        );
        OrderEventEnvelope event = new OrderEventEnvelope(
            "event-77", "ORDER_REFUND_REQUESTED", 1, 1,
            Instant.parse("2026-09-12T06:00:00Z"), "trace-77", "ORDER",
            "wrong-aggregate", 7L, jsonMapper.valueToTree(data)
        );

        assertThrows(
            com.deliveryinsider.report.messaging.order.exception.NonRetryableReportEventException.class,
            () -> service.handle(event)
        );
        verify(refunds, org.mockito.Mockito.never()).insert(org.mockito.ArgumentMatchers.any());
    }
}
