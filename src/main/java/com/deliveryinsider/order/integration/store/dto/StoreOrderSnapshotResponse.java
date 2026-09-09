package com.deliveryinsider.order.integration.store.dto;

import java.util.List;

public record StoreOrderSnapshotResponse(

    Long storeId,

    List<MenuSnapshot> menus

) {

    public StoreOrderSnapshotResponse {
        menus = menus == null
            ? List.of()
            : List.copyOf(menus);
    }

    public record MenuSnapshot(

        Long menuId,

        String menuName,

        long menuPrice,

        long menuCost,

        long packagingCost,

        Integer expectedCookingTime

    ) {
    }
}
