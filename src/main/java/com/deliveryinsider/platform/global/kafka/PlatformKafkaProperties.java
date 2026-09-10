package com.deliveryinsider.platform.global.kafka;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(
    prefix = "deliveryinsider.kafka"
)
public record PlatformKafkaProperties(

    @NotBlank
    String platformOrderTopic,

    @Positive
    long publishTimeoutMs

) {
}
