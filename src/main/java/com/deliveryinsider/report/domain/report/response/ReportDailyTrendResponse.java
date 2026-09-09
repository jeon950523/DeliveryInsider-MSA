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
    long estimatedMenuCost,
    long estimatedPackagingCost,
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
            projection.getEstimatedMenuCost(),
            projection.getEstimatedPackagingCost(),
            parseStatuses(
                projection.getFinancialDataStatusesCsv()
            )
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
