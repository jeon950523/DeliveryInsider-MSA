package com.deliveryinsider.platform.domain.integration;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Simulator catalog DTO only; it is deliberately not a real Provider API contract. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalStoreResponse(
    PlatformType platformType,
    String externalStoreId,
    String storeName,
    boolean enabled
) {
}
