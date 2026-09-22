package com.deliveryinsider.platform.domain.integration;

import com.deliveryinsider.platform.domain.provider.PlatformType;

public record UnresolvedOrderMenuResponse(
    PlatformType platformType,
    String externalStoreId,
    String externalMenuId,
    String menuName,
    Integer price,
    int blockedOrderCount
) {
}
