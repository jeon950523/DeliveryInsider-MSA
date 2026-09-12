package com.deliveryinsider.store.domain.menu.response;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record MenuLossDismissalResponse(
        Long id,
        Long menuId,
        LocalDateTime dismissedAt,
        LocalDateTime hideUntil
) {
}
