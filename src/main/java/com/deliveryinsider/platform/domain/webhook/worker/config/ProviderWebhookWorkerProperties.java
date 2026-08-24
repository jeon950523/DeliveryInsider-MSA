package com.deliveryinsider.platform.domain.webhook.worker.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "webhook.worker")
public record ProviderWebhookWorkerProperties(

    @Min(100)
    long fixedDelayMs,

    @Positive
    long retryDelaySeconds,

    @Min(1)
    int maxRetryCount

) {
}
