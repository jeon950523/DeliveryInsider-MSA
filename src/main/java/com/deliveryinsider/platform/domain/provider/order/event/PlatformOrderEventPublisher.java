package com.deliveryinsider.platform.domain.provider.order.event;

import com.deliveryinsider.platform.domain.webhook.exception.BlockedWebhookProcessingException;
import com.deliveryinsider.platform.domain.webhook.exception.RetryableWebhookProcessingException;
import com.deliveryinsider.platform.global.kafka.PlatformKafkaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class PlatformOrderEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final PlatformKafkaProperties properties;

    public void publish(
        PlatformOrderEvent event
    ) {
        String key = createKey(event);
        String payload = serialize(event);

        try {
            kafkaTemplate
                .send(
                    properties.platformOrderTopic(),
                    key,
                    payload
                )
                .get(
                    properties.publishTimeoutMs(),
                    TimeUnit.MILLISECONDS
                );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new RetryableWebhookProcessingException(
                "KAFKA_PUBLISH_INTERRUPTED",
                "Kafka 이벤트 발행 대기가 중단되었습니다.",
                e
            );

        } catch (
            ExecutionException
            | TimeoutException
            | KafkaException e
        ) {
            throw new RetryableWebhookProcessingException(
                "KAFKA_PUBLISH_FAILED",
                "platform.order-events 발행에 실패했습니다.",
                e
            );
        }
    }

    private String createKey(
        PlatformOrderEvent event
    ) {
        return "%s:%s".formatted(
            event.data()
                .platformType()
                .name(),
            event.data()
                .platformOrderId()
        );
    }

    private String serialize(
        PlatformOrderEvent event
    ) {
        try {
            return jsonMapper.writeValueAsString(event);

        } catch (JacksonException e) {
            throw new BlockedWebhookProcessingException(
                "ORDER_EVENT_SERIALIZATION_FAILED",
                "Platform 주문 이벤트를 직렬화할 수 없습니다.",
                e
            );
        }
    }
}
