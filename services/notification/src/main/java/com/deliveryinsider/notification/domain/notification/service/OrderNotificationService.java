package com.deliveryinsider.notification.domain.notification.service;

import com.deliveryinsider.notification.domain.notification.response.OrderChangeSignal;
import com.deliveryinsider.notification.global.error.BusinessException;
import com.deliveryinsider.notification.global.error.NotificationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendOrderChange(
        OrderChangeSignal signal
    ) {
        try {
            messagingTemplate.convertAndSend(
                "/topic/stores/"
                    + signal.storeId()
                    + "/orders",
                signal
            );

        } catch (RuntimeException e) {
            throw new BusinessException(
                NotificationErrorCode.WEBSOCKET_SIGNAL_FAILED
            );
        }
    }
}
