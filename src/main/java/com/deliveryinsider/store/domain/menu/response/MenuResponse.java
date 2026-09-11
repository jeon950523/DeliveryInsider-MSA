package com.deliveryinsider.store.domain.menu.response;

import com.deliveryinsider.store.domain.menu.enums.MenuStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record MenuResponse(
        Long id,
        String menuName,
        Integer menuPrice,
        Integer menuCost,
        Integer packagingFee,
        Integer expectedCookingTime,
        MenuStatus menuStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
