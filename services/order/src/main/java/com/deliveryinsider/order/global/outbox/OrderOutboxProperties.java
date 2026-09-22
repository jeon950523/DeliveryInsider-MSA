package com.deliveryinsider.order.global.outbox;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "order.outbox")
public record OrderOutboxProperties(

    @NotBlank
    String topic,

    @Min(1)
    int batchSize,

    @Min(100)
    long pollIntervalMs,

    @Min(1)
    int leaseSeconds,

    @Min(1)
    int retryDelaySeconds,

    @Min(100)
    long publishTimeoutMs,

    @Min(1)
    int warnRetryCount

) {
}
