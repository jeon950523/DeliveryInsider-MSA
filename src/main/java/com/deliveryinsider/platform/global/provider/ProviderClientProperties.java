package com.deliveryinsider.platform.global.provider;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "provider-client")
public record ProviderClientProperties(
        @Valid
        Baemin baemin
) {
    public record Baemin(
            @NotBlank
            String baseUrl

    ) {
    }
}
