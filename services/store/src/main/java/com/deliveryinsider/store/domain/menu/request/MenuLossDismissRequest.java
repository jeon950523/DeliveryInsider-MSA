package com.deliveryinsider.store.domain.menu.request;

import jakarta.validation.constraints.Min;

public record MenuLossDismissRequest(
        @Min(value = 1, message = "숨김 기간은 1일 이상이어야 합니다.")
        Integer hideDays
) {
}
