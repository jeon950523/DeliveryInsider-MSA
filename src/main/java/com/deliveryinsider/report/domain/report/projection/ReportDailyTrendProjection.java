package com.deliveryinsider.report.domain.report.projection;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ReportDailyTrendProjection {

    private LocalDate reportDate;

    private long totalOrderCount;
    private long completedOrderCount;
    private long canceledOrderCount;

    private long grossSales;
    private long customerPaidAmount;
    private long providerChargeAmount;
    private long providerFundedDiscountAmount;

    private long estimatedMenuCost;
    private long estimatedPackagingCost;

    private String financialDataStatusesCsv;
}
