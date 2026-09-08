package com.deliveryinsider.report.domain.report.response;

import java.util.List;

public record ReportSummaryResponse(

    long totalOrderCount,
    long completedOrderCount,
    long canceledOrderCount,

    long grossOrderAmount,
    Long customerPaidAmount,

    Long providerChargeAmount,

    long estimatedMenuCost,
    long estimatedPackagingCost,

    List<String> financialDataStatuses
) {
}
