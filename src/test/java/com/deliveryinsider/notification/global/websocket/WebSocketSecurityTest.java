package com.deliveryinsider.notification.global.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.HttpStatus;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketSecurityTest {
    @Test void ticketIsOneUseAndBoundToVerifiedOwner() {
        var store = new WebSocketTicketStore(new WebSocketTicketProperties(Duration.ofSeconds(30)));
        var issued = store.issue(9L, 3L);
        assertEquals(9L, issued.userId()); assertEquals(3L, issued.storeId());
        assertEquals(issued, store.consume(issued.ticket()).orElseThrow());
        assertTrue(store.consume(issued.ticket()).isEmpty());
        assertTrue(store.consume(null).isEmpty()); assertTrue(store.consume(" ").isEmpty());
    }
    @Test void expiredTicketIsRejected() {
        var store = new WebSocketTicketStore(new WebSocketTicketProperties(Duration.ofSeconds(-1)));
        assertTrue(store.consume(store.issue(9L, 3L).ticket()).isEmpty());
    }
    @Test void handshakeIgnoresCallerStoreAndBindsTicketStoreThenRejectsReuse() {
        var store = new WebSocketTicketStore(new WebSocketTicketProperties(Duration.ofSeconds(30)));
        var issued = store.issue(9L, 3L);
        var interceptor = new WebSocketTicketHandshakeInterceptor(store);
        var request = mock(ServerHttpRequest.class); var response = mock(ServerHttpResponse.class);
        when(request.getURI()).thenReturn(URI.create("http://localhost/ws?ticket=" + issued.ticket() + "&storeId=4"));
        Map<String, Object> attributes = new HashMap<>();
        assertTrue(interceptor.beforeHandshake(request, response, null, attributes));
        assertEquals(3L, attributes.get(WebSocketTicketHandshakeInterceptor.STORE_ID_ATTRIBUTE));
        assertEquals(9L, attributes.get(WebSocketTicketHandshakeInterceptor.USER_ID_ATTRIBUTE));
        assertFalse(interceptor.beforeHandshake(request, response, null, new HashMap<>()));
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }
    @Test void onlyExactVerifiedStoreTopicIsAllowed() {
        var interceptor = new StoreSubscriptionAuthorizationInterceptor();
        for (var destination : new String[]{"/topic/stores/3/orders", "/topic/stores/4/orders", "/topic/stores/*/orders", "/topic/orders"}) {
            var headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            headers.setSessionAttributes(Map.of(WebSocketTicketHandshakeInterceptor.STORE_ID_ATTRIBUTE, 3L));
            headers.setDestination(destination);
            var message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
            if (destination.equals("/topic/stores/3/orders")) assertSame(message, interceptor.preSend(message, null));
            else assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, null));
        }
    }
    @Test void unverifiedSessionCannotSubscribe() {
        var headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE); headers.setDestination("/topic/stores/3/orders");
        var message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
        assertThrows(MessageDeliveryException.class, () -> new StoreSubscriptionAuthorizationInterceptor().preSend(message, null));
    }
}
