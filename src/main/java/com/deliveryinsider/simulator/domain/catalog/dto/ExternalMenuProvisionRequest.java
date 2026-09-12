package com.deliveryinsider.simulator.domain.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Control-plane request for simulator fixtures; it is not a real provider API contract. */
public record ExternalMenuProvisionRequest(
    @NotBlank @Size(max = 120) String externalMenuId,
    @NotBlank @Size(max = 120) String catalogKey,
    @NotBlank @Size(max = 160) String menuName,
    @Min(0) long price,
    @NotNull Boolean enabled,
    @Min(0) int sortOrder
) {
}
