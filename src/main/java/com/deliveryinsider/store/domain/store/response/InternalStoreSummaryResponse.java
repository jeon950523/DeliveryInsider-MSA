package com.deliveryinsider.store.domain.store.response;

public record InternalStoreSummaryResponse(
    Long storeId,
    String storeName,
    String openTime,
    String closeTime
) {
}
