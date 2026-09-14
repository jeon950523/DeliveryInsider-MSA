package com.deliveryinsider.report.domain.report.projection;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReportSummaryProjection {

    private long totalOrderCount;
    private long completedOrderCount;
    private long canceledOrderCount;

    private long grossOrderAmount;
    private Long customerPaidAmount;

    private Long providerChargeAmount;

    private long providerFundedDiscountAmount;

    private long estimatedMenuCost;
    private long estimatedPackagingCost;
}
