package com.deliveryinsider.order.domain.order.entity;

import com.deliveryinsider.order.domain.order.model.ProviderChargeType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class OrderProviderChargeEntity {

    private Long id;

    private Long orderId;

    private ProviderChargeType chargeType;

    private long amount;

    private BigDecimal rate;

    private Long basisAmount;

    private boolean provisional;

    private String sourceCode;

    private LocalDateTime createdAt;

    @Builder
    public OrderProviderChargeEntity(
        Long orderId,
        ProviderChargeType chargeType,
        long amount,
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
