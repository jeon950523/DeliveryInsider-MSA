package com.deliveryinsider.order.domain.order.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class OrderItemEntity {

    private Long id;

    private Long orderId;

    private Long menuId;

    private String externalMenuId;

    private String menuNameSnapshot;

    private long menuPriceSnapshot;

    private long menuCostSnapshot;

    private long packagingCostSnapshot;

    private long orderedUnitPrice;

    private int quantity;

    private LocalDateTime createdAt;

    @Builder
    public OrderItemEntity(
        Long orderId,
        Long menuId,
        String externalMenuId,
        String menuNameSnapshot,
        long menuPriceSnapshot,
        long menuCostSnapshot,
        long packagingCostSnapshot,
        long orderedUnitPrice,
        int quantity
    ) {
        this.orderId = orderId;
        this.menuId = menuId;
        this.externalMenuId = externalMenuId;
        this.menuNameSnapshot = menuNameSnapshot;
        this.menuPriceSnapshot = menuPriceSnapshot;
        this.menuCostSnapshot = menuCostSnapshot;
        this.packagingCostSnapshot = packagingCostSnapshot;
        this.orderedUnitPrice = orderedUnitPrice;
        this.quantity = quantity;
    }
}
