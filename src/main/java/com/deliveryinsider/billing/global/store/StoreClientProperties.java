package com.deliveryinsider.billing.global.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "store-client")
public record StoreClientProperties(

    @NotBlank
    String baseUrl,

    @NotBlank
    String internalApiKey,

    @NotNull
    Duration connectTimeout,

    @NotNull
    Duration readTimeout
) {
}
