package com.deliveryinsider.notification.global.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WebSocketTicketStore {

    private final WebSocketTicketProperties properties;

    private final ConcurrentHashMap<String, WebSocketTicket>
        tickets = new ConcurrentHashMap<>();

    public WebSocketTicket issue(
        Long userId,
        Long storeId
    ) {
        cleanupExpired();

        String ticket =
            UUID.randomUUID().toString();

        WebSocketTicket issued =
            new WebSocketTicket(
                ticket,
                userId,
                storeId,
                Instant.now().plus(
                    properties.ticketTtl()
                )
            );

        tickets.put(
            ticket,
            issued
        );

        return issued;
    }

    public Optional<WebSocketTicket> consume(
        String ticket
    ) {
        if (ticket == null
            || ticket.isBlank()) {
            return Optional.empty();
        }

        WebSocketTicket issued =
            tickets.remove(ticket);

        if (issued == null) {
            return Optional.empty();
        }

        if (issued.expiresAt()
            .isBefore(Instant.now())) {
            return Optional.empty();
        }

        return Optional.of(issued);
    }

    private void cleanupExpired() {
        Instant now = Instant.now();

        tickets.entrySet()
            .removeIf(entry ->
                entry.getValue()
                    .expiresAt()
                    .isBefore(now)
            );
    }
}
