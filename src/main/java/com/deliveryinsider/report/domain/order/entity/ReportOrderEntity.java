package com.deliveryinsider.report.domain.order.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ReportOrderEntity {

    private Long orderId;

    private Long storeId;

    private String platformType;
    private String platformOrderId;
    private String externalStoreId;

    private String status;

    private Long sourceSequence;
    private long lastEventVersion;

    private LocalDateTime orderedAt;

    private Long grossOrderAmount;
    private Long customerPaidAmount;
    private Long merchantFundedDiscount;
    private Long providerFundedDiscount;

    private String providerFinancialDataStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public ReportOrderEntity(
        Long orderId,
        Long storeId,
        String platformType,
        String platformOrderId,
        String externalStoreId,
        String status,
        Long sourceSequence,
        long lastEventVersion,
        LocalDateTime orderedAt,
        Long grossOrderAmount,
        Long customerPaidAmount,
        Long merchantFundedDiscount,
        Long providerFundedDiscount,
        String providerFinancialDataStatus
    ) {
        this.orderId = orderId;
        this.storeId = storeId;
        this.platformType = platformType;
        this.platformOrderId = platformOrderId;
        this.externalStoreId = externalStoreId;
        this.status = status;
        this.sourceSequence = sourceSequence;
        this.lastEventVersion = lastEventVersion;
        this.orderedAt = orderedAt;
        this.grossOrderAmount = grossOrderAmount;
        this.customerPaidAmount = customerPaidAmount;
        this.merchantFundedDiscount =
            merchantFundedDiscount;
        this.providerFundedDiscount =
            providerFundedDiscount;
        this.providerFinancialDataStatus =
            providerFinancialDataStatus;
    }
}
