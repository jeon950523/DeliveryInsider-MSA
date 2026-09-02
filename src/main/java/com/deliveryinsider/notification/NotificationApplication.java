package com.deliveryinsider.notification;

import com.deliveryinsider.notification.global.kafka.NotificationKafkaProperties;
import com.deliveryinsider.notification.global.store.StoreClientProperties;
import com.deliveryinsider.notification.global.websocket.WebSocketTicketProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(
    {NotificationKafkaProperties.class, StoreClientProperties.class, WebSocketTicketProperties.class}
)
@SpringBootApplication
public class NotificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
