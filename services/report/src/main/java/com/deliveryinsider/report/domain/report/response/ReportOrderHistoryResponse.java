package com.deliveryinsider.report.domain.report.response;

import com.deliveryinsider.report.domain.report.projection.ReportOrderHistoryProjection;

import java.time.LocalDateTime;

public record ReportOrderHistoryResponse(
    Long orderId,
    String platformType,
    String platformOrderId,
    String historyType,
    String reasonCode,
    String reasonText,
    Long amount,
    String refundStatus,
    LocalDateTime occurredAt
) {
    public static ReportOrderHistoryResponse from(
        ReportOrderHistoryProjection source
    ) {
        return new ReportOrderHistoryResponse(
            source.getOrderId(),
            source.getPlatformType(),
            source.getPlatformOrderId(),
            source.getHistoryType(),
            source.getReasonCode(),
            source.getReasonText(),
            source.getAmount(),
            source.getRefundStatus(),
            source.getOccurredAt()
        );
    }
}
