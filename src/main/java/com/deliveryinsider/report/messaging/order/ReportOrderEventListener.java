package com.deliveryinsider.report.messaging.order;

import com.deliveryinsider.report.application.order.ReportOrderProjectionService;
import com.deliveryinsider.report.application.order.ReportProjectionResult;
import com.deliveryinsider.report.messaging.order.dto.OrderEventEnvelope;
import com.deliveryinsider.report.messaging.order.exception.NonRetryableReportEventException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportOrderEventListener {

    private final JsonMapper jsonMapper;
    private final ReportOrderProjectionService
        projectionService;

    @KafkaListener(
        topics = "${report.kafka.order-event-topic:order.events}",
        groupId = "${report.kafka.order-consumer-group:report-order-event-group}",
        containerFactory =
            "reportOrderEventKafkaListenerContainerFactory"
    )
    public void consume(
        ConsumerRecord<String, String> record
    ) {
        OrderEventEnvelope event =
            deserialize(record.value());

        validateKafkaKey(
            record.key(),
            event
        );

        ReportProjectionResult result =
            projectionService.handle(
                event
            );

        log.info(
            "Order event projected. eventId={}, eventType={}, eventVersion={}, result={}, partition={}, offset={}",
            event.eventId(),
            event.eventType(),
            event.eventVersion(),
            result,
            record.partition(),
            record.offset()
        );
    }

    private OrderEventEnvelope deserialize(
        String payload
    ) {
        try {
            return jsonMapper.readValue(
                payload,
                OrderEventEnvelope.class
            );

        } catch (JacksonException e) {
            throw new NonRetryableReportEventException(
                "Order Event JSON 역직렬화에 실패했습니다.",
                e
            );
        }
    }

    private void validateKafkaKey(
        String kafkaKey,
        OrderEventEnvelope event
    ) {
        if (kafkaKey == null
            || !kafkaKey.equals(
            event.aggregateId()
        )) {
            throw new NonRetryableReportEventException(
                "Order Event Kafka Key가 aggregateId와 일치하지 않습니다."
            );
        }
    }
}
