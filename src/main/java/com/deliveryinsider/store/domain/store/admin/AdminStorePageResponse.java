package com.deliveryinsider.store.domain.store.admin;

import java.util.List;

public record AdminStorePageResponse(
    List<AdminStoreResponse> items,
    long total,
    int page,
    int size
) {
}
