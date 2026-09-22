package com.deliveryinsider.platform.domain.integration;

import com.deliveryinsider.platform.domain.provider.PlatformType;

import java.time.LocalDate;

public record ExternalAdSpendResponse(
    PlatformType platformType,
    String externalStoreId,
    LocalDate spendDate,
    String campaignName,
    long spendAmount
) {
}
