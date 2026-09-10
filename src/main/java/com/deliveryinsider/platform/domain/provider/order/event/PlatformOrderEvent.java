package com.deliveryinsider.platform.domain.provider.order.event;

import java.time.Instant;

public record PlatformOrderEvent(

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
