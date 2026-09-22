package com.deliveryinsider.simulator.domain.financial.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExternalStoreFeePolicyRequest(
    @NotNull @DecimalMin("0.0000") @DecimalMax("100.0000")
    BigDecimal platformCommissionRate,

    @NotNull @DecimalMin("0.0000") @DecimalMax("100.0000")
    BigDecimal paymentFeeRate,

    @Min(0)
    long merchantDeliveryFeeAmount,

    @NotNull
    LocalDateTime effectiveFrom,

    LocalDateTime effectiveTo,

    boolean enabled
) {
}
