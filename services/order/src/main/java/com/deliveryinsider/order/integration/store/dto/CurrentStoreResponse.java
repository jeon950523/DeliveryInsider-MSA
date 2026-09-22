package com.deliveryinsider.order.integration.store.dto;

public record CurrentStoreResponse(
    Long storeId,
    String storeName,
    String openTime,
    String closeTime
) {
}
