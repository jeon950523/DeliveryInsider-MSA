package com.deliveryinsider.order.global.kafka;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "order.kafka")
public record OrderKafkaProperties(

    @NotBlank
    String platformOrderTopic,

    @NotBlank
    String platformOrderDltTopic,

    @PositiveOrZero
    long retryIntervalMs,

    @Min(1)
    int maxAttempts

) {
}
