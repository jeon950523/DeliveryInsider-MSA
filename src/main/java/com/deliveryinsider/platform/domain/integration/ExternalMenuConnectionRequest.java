package com.deliveryinsider.platform.domain.integration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ExternalMenuConnectionRequest(@NotNull @Positive Long menuId) {
    public record CreateAndConnect(
        @NotNull @Min(0) Integer menuCost,
        @NotNull @Min(0) Integer packagingFee,
        @NotNull @Min(1) Integer expectedCookingTime
    ) {
    }
}
