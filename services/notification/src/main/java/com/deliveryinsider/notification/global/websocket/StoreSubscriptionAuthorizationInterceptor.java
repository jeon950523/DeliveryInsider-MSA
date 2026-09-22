package com.deliveryinsider.notification.global.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StoreSubscriptionAuthorizationInterceptor
    implements ChannelInterceptor {

    private static final Pattern ORDER_TOPIC_PATTERN =
        Pattern.compile(
            "^/topic/stores/(\\d+)/orders$"
        );

    @Override
    public Message<?> preSend(
        Message<?> message,
        MessageChannel channel
    ) {
        StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
            );

        if (accessor == null
            || accessor.getCommand()
            != StompCommand.SUBSCRIBE) {
            return message;
        }

        Map<String, Object> attributes =
            accessor.getSessionAttributes();

        if (attributes == null) {
            throw accessDenied();
        }

        Object verifiedStoreId =
            attributes.get(
                WebSocketTicketHandshakeInterceptor
                    .STORE_ID_ATTRIBUTE
            );

        if (!(verifiedStoreId instanceof Long storeId)) {
            throw accessDenied();
        }

        String destination =
            accessor.getDestination();

        if (destination == null) {
            throw accessDenied();
        }

        Matcher matcher =
            ORDER_TOPIC_PATTERN.matcher(
                destination
            );

        if (!matcher.matches()) {
            throw accessDenied();
        }

        Long requestedStoreId =
            Long.valueOf(
                matcher.group(1)
            );

        if (!storeId.equals(
            requestedStoreId
        )) {
            throw accessDenied();
        }

        return message;
    }

    private MessageDeliveryException accessDenied() {
        return new MessageDeliveryException(
            "허용되지 않은 WebSocket 구독입니다."
        );
    }
}
