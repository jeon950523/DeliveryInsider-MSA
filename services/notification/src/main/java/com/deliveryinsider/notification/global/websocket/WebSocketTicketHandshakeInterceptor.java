package com.deliveryinsider.notification.global.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketTicketHandshakeInterceptor
    implements HandshakeInterceptor {

    public static final String USER_ID_ATTRIBUTE =
        "verifiedUserId";

    public static final String STORE_ID_ATTRIBUTE =
        "verifiedStoreId";

    private final WebSocketTicketStore ticketStore;

    @Override
    public boolean beforeHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes
    ) {
        String ticket =
            UriComponentsBuilder
                .fromUri(
                    request.getURI()
                )
                .build()
                .getQueryParams()
                .getFirst("ticket");

        WebSocketTicket verified =
            ticketStore.consume(ticket)
                .orElse(null);

        if (verified == null) {
            response.setStatusCode(
                HttpStatus.UNAUTHORIZED
            );
            return false;
        }

        attributes.put(
            USER_ID_ATTRIBUTE,
            verified.userId()
        );

        attributes.put(
            STORE_ID_ATTRIBUTE,
            verified.storeId()
        );

        return true;
    }

    @Override
    public void afterHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Exception exception
    ) {
    }
}
