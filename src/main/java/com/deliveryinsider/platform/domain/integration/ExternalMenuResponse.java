package com.deliveryinsider.platform.domain.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.deliveryinsider.platform.domain.provider.PlatformType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalMenuResponse(
    PlatformType platformType,
    String externalStoreId,
    String externalMenuId,
    String menuName,
    Integer price,
    boolean enabled
) {
}
