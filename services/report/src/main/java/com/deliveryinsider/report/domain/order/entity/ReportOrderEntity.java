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

    /*
     * 외부 플랫폼 Lifecycle.
     */
    private String status;

    /*
     * 점주/매장 운영 Lifecycle.
     */
    private String operationStatus;

    private Long sourceSequence;

    /*
     * order.events 전체 기준 전역 Event Version.
     */
    private long lastEventVersion;

    private LocalDateTime orderedAt;

    private LocalDateTime cookingStartedAt;
    private LocalDateTime readyForPickupAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime completedAt;
    private LocalDateTime canceledAt;

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
        String operationStatus,
        Long sourceSequence,
        long lastEventVersion,
        LocalDateTime orderedAt,
        LocalDateTime cookingStartedAt,
        LocalDateTime readyForPickupAt,
        LocalDateTime pickedUpAt,
        LocalDateTime completedAt,
        LocalDateTime canceledAt,
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
        this.operationStatus = operationStatus;

        this.sourceSequence = sourceSequence;
        this.lastEventVersion = lastEventVersion;

        this.orderedAt = orderedAt;

        this.cookingStartedAt = cookingStartedAt;
        this.readyForPickupAt = readyForPickupAt;
        this.pickedUpAt = pickedUpAt;
        this.completedAt = completedAt;
        this.canceledAt = canceledAt;

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
