package com.deliveryinsider.report.global.billing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "billing-client")
public record BillingClientProperties(

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
