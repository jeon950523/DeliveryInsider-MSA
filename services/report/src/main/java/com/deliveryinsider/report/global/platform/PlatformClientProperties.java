package com.deliveryinsider.report.global.platform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "platform-client")
public record PlatformClientProperties(
    @NotBlank String baseUrl,
    @NotBlank String internalApiKey,
    @NotNull Duration connectTimeout,
    @NotNull Duration readTimeout
) {
}
