package com.deliveryinsider.order.messaging.order.dto;

import java.time.Instant;

public record OrderDomainEventMessage(

    String eventId,
    String eventType,
    int schemaVersion,
    long eventVersion,

    Instant occurredAt,

    String traceId,

    String aggregateType,
    String aggregateId,

    Long storeId,

    OrderCreatedEventData data
) {
}
