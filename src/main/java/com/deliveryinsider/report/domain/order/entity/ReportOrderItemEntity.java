package com.deliveryinsider.report.domain.order.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReportOrderItemEntity {

    private Long id;

    private Long orderId;
    private Long menuId;

    private String menuName;

    private Long menuPrice;
    private Long menuCost;
    private Long packagingCost;

    private Long orderedUnitPrice;
    private int quantity;

    @Builder
    public ReportOrderItemEntity(
        Long orderId,
        Long menuId,
        String menuName,
        Long menuPrice,
        Long menuCost,
        Long packagingCost,
        Long orderedUnitPrice,
        int quantity
    ) {
        this.orderId = orderId;
        this.menuId = menuId;
        this.menuName = menuName;
        this.menuPrice = menuPrice;
        this.menuCost = menuCost;
        this.packagingCost = packagingCost;
        this.orderedUnitPrice = orderedUnitPrice;
        this.quantity = quantity;
    }
}
