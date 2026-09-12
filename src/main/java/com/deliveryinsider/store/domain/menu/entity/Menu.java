package com.deliveryinsider.store.domain.menu.entity;

import com.deliveryinsider.store.domain.menu.enums.MenuStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Menu {

    private Long id;
    private Long eventVersion;

    // 해당 메뉴를 소유한 매장 PK
    private Long storeId;

    private String menuName;

    // 메뉴 판매가
    private Integer menuPrice;

    // 메뉴 원가
    private Integer menuCost;

    // 포장 용기 비용
    private Integer packagingFee;

    // 예상 조리시간 (분 단위)
    private Integer expectedCookingTime;

    private MenuStatus menuStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
