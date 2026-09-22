package com.deliveryinsider.notification.global.websocket;

import java.time.Instant;

public record WebSocketTicket(
    String ticket,
    Long userId,
    Long storeId,
    Instant expiresAt
) {
}
