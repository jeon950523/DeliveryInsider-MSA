package com.deliveryinsider.report.domain.report.response;

import com.deliveryinsider.report.domain.report.projection.ReportPlatformMetricProjection;

public record ReportPlatformMetricResponse(
    String platformType,
    long totalOrderCount,
    long completedOrderCount,
    long canceledOrderCount,
    long grossSales
) {

    public static ReportPlatformMetricResponse from(
        ReportPlatformMetricProjection projection
    ) {
        return new ReportPlatformMetricResponse(
            projection.getPlatformType(),
            projection.getTotalOrderCount(),
            projection.getCompletedOrderCount(),
            projection.getCanceledOrderCount(),
            projection.getGrossSales()
        );
    }
}
