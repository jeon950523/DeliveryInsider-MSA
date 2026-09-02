package com.deliveryinsider.notification.global.kafka;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "notification.kafka")
public record NotificationKafkaProperties(

    @NotBlank
    String orderEventTopic,

    @NotBlank
    String orderEventDltTopic,

    @NotBlank
    String orderConsumerGroup,

    @Min(1)
    long retryIntervalMs,

    @Min(1)
    int maxAttempts

) {
}
