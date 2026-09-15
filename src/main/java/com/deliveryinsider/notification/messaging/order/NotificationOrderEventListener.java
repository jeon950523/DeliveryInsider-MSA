package com.deliveryinsider.notification.messaging.order;

import com.deliveryinsider.notification.domain.notification.response.OrderChangeSignal;
import com.deliveryinsider.notification.domain.notification.service.OrderNotificationService;
import com.deliveryinsider.notification.global.kafka.EventEnvelope;
import com.deliveryinsider.notification.messaging.order.exception.NonRetryableNotificationEventException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOrderEventListener {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final JsonMapper jsonMapper;

    private final OrderNotificationService
        notificationService;

    @KafkaListener(
        topics = "${notification.kafka.order-event-topic}",
        groupId = "${notification.kafka.order-consumer-group}",
        containerFactory =
            "notificationOrderEventKafkaListenerContainerFactory"
    )
    public void consume(
        ConsumerRecord<String, String> record
    ) {
        EventEnvelope<JsonNode> event =
            deserialize(
                record.value()
            );

        validate(
            record.key(),
            event
        );

        Long orderId =
            parseOrderId(
                event.aggregateId()
            );

        notificationService.sendOrderChange(
            new OrderChangeSignal(
                event.eventId(),
                event.eventType(),
                event.eventVersion(),
                event.storeId(),
                orderId,
                event.occurredAt()
            )
        );

        log.info(
            "Order notification signal sent. eventId={}, eventType={}, orderId={}, storeId={}, partition={}, offset={}",
            event.eventId(),
            event.eventType(),
            orderId,
            event.storeId(),
            record.partition(),
            record.offset()
        );
    }

    private EventEnvelope<JsonNode> deserialize(
        String payload
    ) {
        try {
            return jsonMapper.readValue(
                payload,
                new TypeReference<
                    EventEnvelope<JsonNode>
                    >() {
                }
            );

        } catch (JacksonException e) {
            throw new NonRetryableNotificationEventException(
                "Order Event JSON 역직렬화에 실패했습니다.",
                e
            );
        }
    }

    private void validate(
        String kafkaKey,
        EventEnvelope<JsonNode> event
    ) {
        if (event == null
            || event.eventId() == null
            || event.eventType() == null
            || event.aggregateId() == null
            || event.storeId() == null
            || event.eventVersion() == null
            || event.eventVersion() < 1) {

            throw new NonRetryableNotificationEventException(
                "Order Event Envelope가 올바르지 않습니다."
            );
        }

        if (event.schemaVersion() == null
            || event.schemaVersion()
            != SUPPORTED_SCHEMA_VERSION) {

            throw new NonRetryableNotificationEventException(
                "지원하지 않는 Order Event Schema입니다."
            );
        }

        if (!event.aggregateId().equals(
            kafkaKey
        )) {
            throw new NonRetryableNotificationEventException(
                "Kafka Key와 Order aggregateId가 일치하지 않습니다."
            );
        }

        if (!isSupportedEventType(
            event.eventType()
        )) {
            throw new NonRetryableNotificationEventException(
                "지원하지 않는 Order Event입니다. eventType="
                    + event.eventType()
            );
        }
    }

    private boolean isSupportedEventType(
        String eventType
    ) {
        return switch (eventType) {

            case "ORDER_CREATED",
                 "ORDER_STATUS_CHANGED",
                 "ORDER_OPERATION_STATUS_CHANGED",
                 "ORDER_CANCELED",
                 "ORDER_REFUND_REQUESTED",
                 "ORDER_REFUNDED" ->
                true;

            default ->
                false;
        };
    }

    private Long parseOrderId(
        String aggregateId
    ) {
        try {
            return Long.valueOf(
                aggregateId
            );

        } catch (NumberFormatException e) {
            throw new NonRetryableNotificationEventException(
                "Order aggregateId가 숫자가 아닙니다.",
                e
            );
        }
    }
}
