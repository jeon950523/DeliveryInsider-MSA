package com.deliveryinsider.platform.domain.integration;

import com.deliveryinsider.platform.domain.provider.PlatformType;

import java.util.List;

/**
 * Internal-only result.  available=false means Report must not present an
 * unknown provider expense as a confirmed zero amount.
 */
public record PlatformAdSpendResponse(
    PlatformType platformType,
    String externalStoreId,
    boolean available,
    List<ExternalAdSpendResponse> spends
) {
    public PlatformAdSpendResponse {
        spends = spends == null ? List.of() : List.copyOf(spends);
    }
}
