package com.deliveryinsider.store.domain.menu.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InternalMenuCreateRequest(
    @NotBlank @Size(max = 180) @Pattern(regexp = "[A-Za-z0-9:_-]+")
    String operationKey,
    @NotBlank @Size(max = 100) @Pattern(regexp = "(?s).*\\S.*")
    String menuName,
    @NotNull @Min(0) Integer menuPrice,
    @NotNull @Min(0) Integer menuCost,
    @NotNull @Min(0) Integer packagingFee,
    @NotNull @Min(1) Integer expectedCookingTime
) {
}
