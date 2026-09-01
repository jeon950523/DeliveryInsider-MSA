package com.deliveryinsider.report.domain.order.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ReportOrderChargeEntity {

    private Long id;

    private Long orderId;

    private String chargeType;

    private Long amount;
    private BigDecimal rate;
    private Long basisAmount;

    private boolean provisional;

    private String sourceCode;

    @Builder
    public ReportOrderChargeEntity(
        Long orderId,
        String chargeType,
        Long amount,
        BigDecimal rate,
        Long basisAmount,
        boolean provisional,
        String sourceCode
    ) {
        this.orderId = orderId;
        this.chargeType = chargeType;
        this.amount = amount;
        this.rate = rate;
        this.basisAmount = basisAmount;
        this.provisional = provisional;
        this.sourceCode = sourceCode;
    }
}
