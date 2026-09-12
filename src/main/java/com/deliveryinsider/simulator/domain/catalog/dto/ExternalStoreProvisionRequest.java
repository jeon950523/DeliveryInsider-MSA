package com.deliveryinsider.simulator.domain.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Control-plane request for simulator fixtures; it is not a real provider API contract. */
public record ExternalStoreProvisionRequest(
    @NotBlank @Size(max = 120) String externalStoreId,
    @NotBlank @Size(max = 120) String storeName,
    @NotNull Boolean enabled
) {
}
