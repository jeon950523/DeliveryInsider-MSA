package com.deliveryinsider.report.messaging.order.dto;

import tools.jackson.databind.JsonNode;
import java.time.Instant;

public record OrderEventEnvelope(
    String eventId,
    String eventType,
    int schemaVersion,
    long eventVersion,
    Instant occurredAt,
    String traceId,
    String aggregateType,
    String aggregateId,
    Long storeId,
    JsonNode data
) {
}
