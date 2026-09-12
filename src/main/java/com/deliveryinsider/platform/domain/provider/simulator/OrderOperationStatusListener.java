package com.deliveryinsider.platform.domain.provider.simulator;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The merchant-owned READY_FOR_PICKUP transition opens the simulator's
 * provider-owned delivery transitions. The order event is the only boundary;
 * Platform never writes the Order database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderOperationStatusListener {

    private final JsonMapper jsonMapper;
    private final SimulatorDeliveryControlClient deliveryControlClient;

    @KafkaListener(
        topics = "${order-operation.consumer.topic}",
        groupId = "${order-operation.consumer.group}"
    )
    public void receive(String payload) {
        try {
            JsonNode event = jsonMapper.readTree(payload);
            if (!"ORDER_OPERATION_STATUS_CHANGED".equals(event.path("eventType").asText())
                || !"READY_FOR_PICKUP".equals(event.path("data").path("operationStatus").asText())) {
                return;
            }

            PlatformType platformType = PlatformType.valueOf(
                event.path("data").path("platformType").asText()
            );
            String platformOrderId = event.path("data").path("platformOrderId").asText();
            if (platformOrderId.isBlank()) {
                log.warn("Simulator readiness sync ignored: platform order identity is missing");
                return;
            }

            deliveryControlClient.markReadyForPickup(platformType, platformOrderId);
            log.info("Simulator pickup readiness synchronized. platformType={}, platformOrderId={}",
                platformType, platformOrderId);
        } catch (Exception exception) {
            /* Do not acknowledge a transient provider failure: Kafka must retry this readiness command. */
            log.warn("Simulator readiness sync failed. exceptionClass={}", exception.getClass().getSimpleName());
            throw new IllegalStateException("Simulator pickup readiness synchronization failed", exception);
        }
    }
}
