package com.deliveryinsider.simulator.domain.financial.model;

import com.deliveryinsider.simulator.domain.provider.PlatformType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExternalStoreFeePolicy(
    PlatformType platformType,
    String externalStoreId,
    BigDecimal platformCommissionRate,
    BigDecimal paymentFeeRate,
    long merchantDeliveryFeeAmount,
    LocalDateTime effectiveFrom,
    LocalDateTime effectiveTo,
    boolean enabled
) {
}
