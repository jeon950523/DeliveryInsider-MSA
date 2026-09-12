package com.deliveryinsider.report.domain.report.projection;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ReportOrderHistoryProjection {

    private Long orderId;
    private String platformType;
    private String platformOrderId;
    private String historyType;
    private String reasonCode;
    private String reasonText;
    private Long amount;
    private String refundStatus;
    private LocalDateTime occurredAt;
}
