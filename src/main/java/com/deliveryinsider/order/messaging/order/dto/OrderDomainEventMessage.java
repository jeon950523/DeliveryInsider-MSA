package com.deliveryinsider.order.messaging.order.dto;

import java.time.Instant;

public record OrderDomainEventMessage<T>(

    String eventId,
    String eventType,
    int schemaVersion,
    long eventVersion,

    Instant occurredAt,

    String traceId,

    String aggregateType,
    String aggregateId,

    Long storeId,

    T data
) {
}
