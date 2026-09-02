package com.deliveryinsider.notification.domain.notification.service;

import com.deliveryinsider.notification.global.websocket.WebSocketTicket;
import com.deliveryinsider.notification.global.websocket.WebSocketTicketStore;
import com.deliveryinsider.notification.integration.store.CurrentStoreClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketTicketService {

    private final CurrentStoreClient currentStoreClient;
    private final WebSocketTicketStore ticketStore;

    public WebSocketTicket issue(
        Long userId
    ) {
        var store =
            currentStoreClient.findByUserId(
                userId
            );

        return ticketStore.issue(
            userId,
            store.storeId()
        );
    }
}
