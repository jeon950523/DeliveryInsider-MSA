package com.deliveryinsider.platform.domain.integration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PlatformIntegrationRequest(
    @NotBlank @Size(max = 150) @Pattern(regexp = "[^\\p{Cntrl}]+") String externalStoreId,
    @NotNull Boolean enabled,
    @NotBlank @Pattern(regexp = "SIMULATOR|SANDBOX") String environment
) {
    public record Enabled(@NotNull Boolean enabled) {}
    public record Menu(@NotBlank @Size(max = 150) @Pattern(regexp = "[^\\p{Cntrl}]+") String externalMenuId,
                       @NotNull Boolean enabled) {}
}
