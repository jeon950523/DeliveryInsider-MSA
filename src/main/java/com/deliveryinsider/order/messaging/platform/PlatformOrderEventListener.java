package com.deliveryinsider.order.messaging.platform;

import com.deliveryinsider.order.application.order.OrderEventHandlingResult;
import com.deliveryinsider.order.application.order.PlatformOrderCreatedApplicationService;
import com.deliveryinsider.order.messaging.platform.dto.PlatformOrderEventMessage;
import com.deliveryinsider.order.messaging.platform.exception.NonRetryableOrderEventProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformOrderEventListener {

    private final JsonMapper jsonMapper;

    private final PlatformOrderCreatedApplicationService
        orderCreatedService;

    @KafkaListener(
        topics = "${order.kafka.platform-order-topic}"
    )
    public void consume(
        ConsumerRecord<String, String> record
    ) {
        PlatformOrderEventMessage message =
            deserialize(record.value());

        validateKafkaKey(
            record.key(),
            message
        );

        OrderEventHandlingResult result =
            orderCreatedService.handle(
                message
            );

        log.info(
            "Platform order event handled. eventType={}, result={}, partition={}, offset={}",
            message.eventType(),
            result,
            record.partition(),
            record.offset()
        );
    }

    private PlatformOrderEventMessage deserialize(
        String payload
    ) {
        try {
            return jsonMapper.readValue(
                payload,
                PlatformOrderEventMessage.class
            );

        } catch (JacksonException e) {
            throw new NonRetryableOrderEventProcessingException(
                "PLATFORM_EVENT_DESERIALIZATION_FAILED",
                "Platform 주문 이벤트 JSON을 역직렬화할 수 없습니다.",
                e
            );
        }
    }

    private void validateKafkaKey(
        String key,
        PlatformOrderEventMessage message
    ) {
        if (
            message == null
                || message.data() == null
                || message.data().platformType() == null
                || message.data().platformOrderId() == null
        ) {
            throw new NonRetryableOrderEventProcessingException(
                "PLATFORM_EVENT_IDENTITY_MISSING",
                "Platform 주문 이벤트 식별 정보가 없습니다."
            );
        }

        String expectedKey =
            "%s:%s".formatted(
                message.data()
                    .platformType()
                    .name(),
                message.data()
                    .platformOrderId()
            );

        if (!Objects.equals(
            expectedKey,
            key
        )) {
            throw new NonRetryableOrderEventProcessingException(
                "PLATFORM_EVENT_KEY_MISMATCH",
                "Kafka Key와 Platform 주문 Identity가 일치하지 않습니다."
            );
        }
    }
}
