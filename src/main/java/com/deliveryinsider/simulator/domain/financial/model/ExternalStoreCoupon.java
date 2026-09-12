package com.deliveryinsider.simulator.domain.financial.model;

import com.deliveryinsider.simulator.domain.provider.PlatformType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExternalStoreCoupon(
    String couponId,
    PlatformType platformType,
    String externalStoreId,
    String code,
    String name,
    CouponDiscountType discountType,
    BigDecimal discountValue,
    Long maxDiscountAmount,
    CouponFundingType fundingType,
    BigDecimal merchantShareRate,
    LocalDateTime activeFrom,
    LocalDateTime activeTo,
    boolean enabled
) {
}
