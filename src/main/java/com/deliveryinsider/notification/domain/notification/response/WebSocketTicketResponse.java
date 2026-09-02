package com.deliveryinsider.notification.domain.notification.response;

import com.deliveryinsider.notification.global.websocket.WebSocketTicket;

import java.time.Instant;

public record WebSocketTicketResponse(
    String ticket,
    Long storeId,
    Instant expiresAt
) {

    public static WebSocketTicketResponse from(
        WebSocketTicket ticket
    ) {
        return new WebSocketTicketResponse(
            ticket.ticket(),
            ticket.storeId(),
            ticket.expiresAt()
        );
    }
}
