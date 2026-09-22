package com.deliveryinsider.store.domain.menu.entity;

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
public class MenuLossDismissal {

    private Long id;

    private Long storeId;

    private Long menuId;

    private LocalDateTime dismissedAt;

    // 언제까지 숨길 것인지 (예: 7일 후)
    private LocalDateTime hideUntil;

    private LocalDateTime restoredAt;
}
