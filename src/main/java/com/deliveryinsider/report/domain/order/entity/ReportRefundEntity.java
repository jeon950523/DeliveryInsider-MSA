package com.deliveryinsider.report.domain.order.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReportRefundEntity {

    private Long orderId;
    private String providerRefundId;
    private String status;
    private long amount;
    private String reasonCode;
    private String reasonText;
    private LocalDateTime requestedAt;
    private long eventVersion;
}
