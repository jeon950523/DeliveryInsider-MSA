package com.deliveryinsider.report.domain.report.service;

import java.time.LocalDateTime;
import java.util.List;

public record ReportAiInsightContext(

    LocalDateTime from,

    LocalDateTime to,

    String platformType,

    long totalOrderCount,

    long completedOrderCount,

    long canceledOrderCount,

    long grossOrderAmount,

    boolean financialDataAvailable,

    boolean cancellationReasonAvailable,

    Metric totalProcessing,

    Metric waiting,

    Metric cooking,

    Metric pickupWaiting,

    Metric delivery,

    List<PlatformMetric> platforms

) {

    public record Metric(
        long sampleCount,
        Long averageSeconds
    ) {
    }

    public record PlatformMetric(
        String platformType,
        long completedOrderCount,
        Metric totalProcessing
    ) {
    }
}
