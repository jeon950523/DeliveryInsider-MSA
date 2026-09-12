package com.deliveryinsider.store.domain.store.response;

import java.util.List;

public record StoreOrderSnapshotResponse(
    Long storeId,
    List<MenuSnapshot> menus
) {

    public record MenuSnapshot(
        Long menuId,
        String menuName,
        Integer menuPrice,
        Integer menuCost,
        Integer packagingCost,
        Integer expectedCookingTime
    ) {
    }
}
