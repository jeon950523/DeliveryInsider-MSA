package com.deliveryinsider.simulator.domain.financial.dto;

import com.deliveryinsider.simulator.domain.provider.PlatformType;

import java.time.LocalDate;

public record ExternalAdSpendResponse(
    PlatformType platformType,
    String externalStoreId,
    LocalDate spendDate,
    String campaignName,
    long spendAmount
) {
}
