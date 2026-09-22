package com.deliveryinsider.simulator.domain.financial.dto;

import com.deliveryinsider.simulator.domain.financial.model.CouponDiscountType;
import com.deliveryinsider.simulator.domain.financial.model.CouponFundingType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExternalStoreCouponRequest(
    @NotBlank @Size(max = 120)
    String code,

    @NotBlank @Size(max = 160)
    String name,

    @NotNull
    CouponDiscountType discountType,

    @NotNull @DecimalMin("0.0000")
    BigDecimal discountValue,

    Long maxDiscountAmount,

    @NotNull
    CouponFundingType fundingType,

    @DecimalMin("0.0000") @DecimalMax("100.0000")
    BigDecimal merchantShareRate,

    @NotNull
    LocalDateTime activeFrom,

    LocalDateTime activeTo,

    boolean enabled
) {
}
