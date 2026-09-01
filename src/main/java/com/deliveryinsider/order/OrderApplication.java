package com.deliveryinsider.order;

import com.deliveryinsider.order.global.kafka.OrderKafkaProperties;
import com.deliveryinsider.order.global.outbox.OrderOutboxProperties;
import com.deliveryinsider.order.global.store.StoreClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties({StoreClientProperties.class, OrderKafkaProperties.class, OrderOutboxProperties.class})
@SpringBootApplication
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
