package com.deliveryinsider.notification.global.websocket;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(
    prefix = "notification.websocket"
)
public record WebSocketTicketProperties(

    @NotNull
    Duration ticketTtl

) {
}
