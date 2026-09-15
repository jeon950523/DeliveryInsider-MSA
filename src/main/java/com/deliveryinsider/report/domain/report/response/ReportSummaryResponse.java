package com.deliveryinsider.report.domain.report.response;

import java.util.List;

public record ReportSummaryResponse(

    long totalOrderCount,
    long completedOrderCount,
    long canceledOrderCount,

    long grossOrderAmount,
    Long customerPaidAmount,

    Long providerChargeAmount,
    long providerFundedDiscountAmount,

    long estimatedMenuCost,
    long estimatedPackagingCost,
    long estimatedNetProfit,

    List<String> financialDataStatuses,
    long customerRefundAmount,
    long merchantLiabilityAmount,
    long platformLiabilityAmount,
    long netSales
) {
    public ReportSummaryResponse(
        long totalOrderCount,
        long completedOrderCount,
        long canceledOrderCount,
        long grossOrderAmount,
        Long customerPaidAmount,
        Long providerChargeAmount,
        long estimatedMenuCost,
        long estimatedPackagingCost,
        long estimatedNetProfit,
        List<String> financialDataStatuses
    ) {
        this(
            totalOrderCount,
            completedOrderCount,
            canceledOrderCount,
            grossOrderAmount,
            customerPaidAmount,
            providerChargeAmount,
            0,
            estimatedMenuCost,
            estimatedPackagingCost,
            estimatedNetProfit,
            financialDataStatuses,
            0,
            0,
            0,
            grossOrderAmount
        );
    }
}
