package com.deliveryinsider.report.domain.report.response;

import java.util.List;

public record ReportSummaryResponse(

    long totalOrderCount,
    long completedOrderCount,
    long canceledOrderCount,

    long grossOrderAmount,
    long customerPaidAmount,

    long providerChargeAmount,

    long estimatedMenuCost,
    long estimatedPackagingCost,

    List<String> financialDataStatuses
) {
}
