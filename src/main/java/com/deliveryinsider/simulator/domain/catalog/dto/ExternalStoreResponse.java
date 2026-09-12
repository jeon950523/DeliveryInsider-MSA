package com.deliveryinsider.simulator.domain.catalog.dto;

import com.deliveryinsider.simulator.domain.provider.PlatformType;

public record ExternalStoreResponse(
    PlatformType platformType,
    String externalStoreId,
    String storeName,
    boolean enabled
) {
}
