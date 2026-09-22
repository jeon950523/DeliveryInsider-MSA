package com.deliveryinsider.report.domain.report.response;

import com.deliveryinsider.report.domain.report.projection.ReportMenuPerformanceProjection;

public record ReportMenuPerformanceResponse(
    Long menuId,
    String menuName,
    long orderCount,
    long soldQuantity,
    long grossSales,
    long estimatedMenuCost,
    long estimatedPackagingCost
) {

    public static ReportMenuPerformanceResponse from(
        ReportMenuPerformanceProjection projection
    ) {
        return new ReportMenuPerformanceResponse(
            projection.getMenuId(),
            projection.getMenuName(),
            projection.getOrderCount(),
            projection.getSoldQuantity(),
            projection.getGrossSales(),
            projection.getEstimatedMenuCost(),
            projection.getEstimatedPackagingCost()
        );
    }
}
