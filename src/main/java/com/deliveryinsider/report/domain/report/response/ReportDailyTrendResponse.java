package com.deliveryinsider.report.domain.report.response;

import com.deliveryinsider.report.domain.report.projection.ReportDailyTrendProjection;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public record ReportDailyTrendResponse(
    LocalDate reportDate,
    long totalOrderCount,
    long completedOrderCount,
    long canceledOrderCount,
    long grossSales,
    long customerPaidAmount,
    long providerChargeAmount,
    long providerFundedDiscountAmount,
    long estimatedMenuCost,
    long estimatedPackagingCost,
    long estimatedNetProfit,
    List<String> financialDataStatuses
) {

    public static ReportDailyTrendResponse from(
        ReportDailyTrendProjection projection
    ) {
        return new ReportDailyTrendResponse(
            projection.getReportDate(),
            projection.getTotalOrderCount(),
            projection.getCompletedOrderCount(),
            projection.getCanceledOrderCount(),
            projection.getGrossSales(),
            projection.getCustomerPaidAmount(),
            projection.getProviderChargeAmount(),
            projection.getProviderFundedDiscountAmount(),
            projection.getEstimatedMenuCost(),
            projection.getEstimatedPackagingCost(),
            projection.getGrossSales()
                - projection.getProviderChargeAmount()
                + projection.getProviderFundedDiscountAmount()
                - projection.getEstimatedMenuCost()
                - projection.getEstimatedPackagingCost(),
            parseStatuses(
                projection.getFinancialDataStatusesCsv()
            )
        );
    }

    public ReportDailyTrendResponse(
        LocalDate reportDate,
        long totalOrderCount,
        long completedOrderCount,
        long canceledOrderCount,
        long grossSales,
        long customerPaidAmount,
        long providerChargeAmount,
        long estimatedMenuCost,
        long estimatedPackagingCost,
        long estimatedNetProfit,
        List<String> financialDataStatuses
    ) {
        this(
            reportDate,
            totalOrderCount,
            completedOrderCount,
            canceledOrderCount,
            grossSales,
            customerPaidAmount,
            providerChargeAmount,
            0,
            estimatedMenuCost,
            estimatedPackagingCost,
            estimatedNetProfit,
            financialDataStatuses
        );
    }

    private static List<String> parseStatuses(
        String statusesCsv
    ) {
        if (statusesCsv == null
            || statusesCsv.isBlank()) {
            return List.of();
        }

        return Arrays.stream(
                statusesCsv.split(",")
            )
            .map(String::trim)
            .filter(status -> !status.isBlank())
            .distinct()
            .toList();
    }
}
