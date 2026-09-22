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

    /*
     * 주문 당시 Store 메뉴 기준 조리시간.
     *
     * Menu가 나중에 변경돼도
     * 과거 주문 지연 계산은 이 Snapshot을 사용한다.
     */
    private Integer expectedCookingTimeSnapshot;

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
        Integer expectedCookingTimeSnapshot,
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
        this.expectedCookingTimeSnapshot =
            expectedCookingTimeSnapshot;
        this.orderedUnitPrice = orderedUnitPrice;
        this.quantity = quantity;
    }
}
