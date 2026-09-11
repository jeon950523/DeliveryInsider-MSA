package com.deliveryinsider.store.domain.store.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record StoreOrderSnapshotRequest(

    @NotEmpty(message = "조회할 메뉴가 하나 이상 필요합니다.")
    List<
        @NotNull(message = "메뉴 ID는 필수입니다.")
        @Positive(message = "메뉴 ID는 1 이상이어야 합니다.")
            Long
        > menuIds

) {
}
