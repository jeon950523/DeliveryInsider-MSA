package com.deliveryinsider.store.domain.store.admin;

public record AdminStoreSummaryResponse(
    long totalStoreCount,
    long operatingStoreCount,
    long closedStoreCount,
    long businessVerifiedStoreCount
) {
}
