package com.deliveryinsider.platform.domain.provider.order.model;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ProviderOrderCharge(

    ProviderChargeType chargeType,
    long amount,
    BigDecimal rate,
    Long basisAmount,
    boolean provisional,
    String sourceCode

) {
}
