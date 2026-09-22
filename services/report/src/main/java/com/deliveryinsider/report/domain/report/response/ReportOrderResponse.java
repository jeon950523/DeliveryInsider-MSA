package com.deliveryinsider.report.domain.report.response;

import com.deliveryinsider.report.domain.report.projection.ReportOrderProjection;

import java.time.LocalDateTime;

public record ReportOrderResponse(
    Long orderId,
    String platformType,
    String platformOrderId,
    String status,
    LocalDateTime orderedAt,
    Long grossOrderAmount,
    Long customerPaidAmount,
    String financialDataStatus
) {

    public static ReportOrderResponse from(
        ReportOrderProjection projection
    ) {
        return new ReportOrderResponse(
            projection.getOrderId(),
            projection.getPlatformType(),
            projection.getPlatformOrderId(),
            projection.getStatus(),
            projection.getOrderedAt(),
            projection.getGrossOrderAmount(),
            projection.getCustomerPaidAmount(),
            projection.getFinancialDataStatus()
        );
    }
}
