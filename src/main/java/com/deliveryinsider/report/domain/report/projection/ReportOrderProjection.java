package com.deliveryinsider.report.domain.report.projection;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ReportOrderProjection {

    private Long orderId;

    private String platformType;
    private String platformOrderId;

    private String status;

    private LocalDateTime orderedAt;

    private Long grossOrderAmount;
    private Long customerPaidAmount;

    private String financialDataStatus;
}
