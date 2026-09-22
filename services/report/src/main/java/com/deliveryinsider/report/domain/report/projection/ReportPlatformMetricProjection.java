package com.deliveryinsider.report.domain.report.projection;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReportPlatformMetricProjection {

    private String platformType;
    private long totalOrderCount;
    private long completedOrderCount;
    private long canceledOrderCount;
    private long grossSales;
}
