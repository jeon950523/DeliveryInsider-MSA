package com.deliveryinsider.order.messaging.platform.dto;

import java.time.Instant;

public record PlatformOrderEventMessage(

    String eventId,
    String eventType,
    int schemaVersion,
    Long eventVersion,

    Instant occurredAt,

    String traceId,

    String aggregateType,
    String aggregateId,

    Long storeId,

    PlatformOrderEventData data

) {
}
