package com.deliveryinsider.report.domain.report.response;

import com.deliveryinsider.report.domain.report.projection.ReportPlatformProcessingTimeProjection;
import com.deliveryinsider.report.domain.report.projection.ReportProcessingTimeProjection;

import java.util.List;

public record ReportProcessingTimeResponse(

    long completedOrderCount,

    Metric totalProcessing,

    Metric waiting,

    Metric cooking,

    Metric pickupWaiting,

    Metric delivery,

    List<Platform> platforms

) {

    public static ReportProcessingTimeResponse from(
        ReportProcessingTimeProjection summary,
        List<ReportPlatformProcessingTimeProjection> platforms
    ) {
        return new ReportProcessingTimeResponse(
            summary.getCompletedOrderCount(),

            new Metric(
                summary.getTotalProcessingSampleCount(),
                summary.getAverageTotalProcessingSeconds()
            ),

            new Metric(
                summary.getWaitingSampleCount(),
                summary.getAverageWaitingSeconds()
            ),

            new Metric(
                summary.getCookingSampleCount(),
                summary.getAverageCookingSeconds()
            ),

            new Metric(
                summary.getPickupWaitingSampleCount(),
                summary.getAveragePickupWaitingSeconds()
            ),

            new Metric(
                summary.getDeliverySampleCount(),
                summary.getAverageDeliverySeconds()
            ),

            platforms.stream()
                .map(Platform::from)
                .toList()
        );
    }

    public record Metric(
        long sampleCount,
        Long averageSeconds
    ) {
    }

    public record Platform(

        String platformType,

        long completedOrderCount,

        Metric totalProcessing,

        Metric waiting,

        Metric cooking,

        Metric pickupWaiting,

        Metric delivery

    ) {

        private static Platform from(
            ReportPlatformProcessingTimeProjection projection
        ) {
            return new Platform(
                projection.getPlatformType(),

                projection.getCompletedOrderCount(),

                new Metric(
                    projection.getTotalProcessingSampleCount(),
                    projection.getAverageTotalProcessingSeconds()
                ),

                new Metric(
                    projection.getWaitingSampleCount(),
                    projection.getAverageWaitingSeconds()
                ),

                new Metric(
                    projection.getCookingSampleCount(),
                    projection.getAverageCookingSeconds()
                ),

                new Metric(
                    projection.getPickupWaitingSampleCount(),
                    projection.getAveragePickupWaitingSeconds()
                ),

                new Metric(
                    projection.getDeliverySampleCount(),
                    projection.getAverageDeliverySeconds()
                )
            );
        }
    }
}
