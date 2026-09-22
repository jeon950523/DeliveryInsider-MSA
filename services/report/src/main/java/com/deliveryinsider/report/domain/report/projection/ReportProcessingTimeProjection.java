package com.deliveryinsider.report.domain.report.projection;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReportProcessingTimeProjection {

    private long completedOrderCount;

    private long totalProcessingSampleCount;
    private Long averageTotalProcessingSeconds;

    private long waitingSampleCount;
    private Long averageWaitingSeconds;

    private long cookingSampleCount;
    private Long averageCookingSeconds;

    private long pickupWaitingSampleCount;
    private Long averagePickupWaitingSeconds;

    private long deliverySampleCount;
    private Long averageDeliverySeconds;
}
