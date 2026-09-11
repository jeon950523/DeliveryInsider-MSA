package com.deliveryinsider.simulator.domain.catalog.dto;

import com.deliveryinsider.simulator.domain.provider.PlatformType;

public record ExternalMenuResponse(
    PlatformType platformType,
    String externalStoreId,
    String externalMenuId,
    String catalogKey,
    String menuName,
    long price,
    boolean enabled
) {
}
