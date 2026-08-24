package com.deliveryinsider.simulator.global.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "provider")
public record ProviderProperties(
    @NotNull @Valid Baemin baemin
) {

    public record Baemin(
        @NotBlank String webhookSecret
    ) {
    }
}
