package com.deliveryinsider.report.domain.order.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ReportCancellationEntity {

    private Long id;
    private Long orderId;

    private String providerCancelCode;
    private String providerCancelReason;

    private LocalDateTime canceledAt;

    private long eventVersion;

    @Builder
    public ReportCancellationEntity(
        Long orderId,
        String providerCancelCode,
        String providerCancelReason,
        LocalDateTime canceledAt,
        long eventVersion
    ) {
        this.orderId = orderId;
        this.providerCancelCode = providerCancelCode;
        this.providerCancelReason = providerCancelReason;
        this.canceledAt = canceledAt;
        this.eventVersion = eventVersion;
    }
}
