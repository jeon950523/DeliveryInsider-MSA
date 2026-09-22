package com.deliveryinsider.store.global.kafka;

import java.time.Instant;

public record EventEnvelope<T>(
    String eventId,
    String eventType,
    Integer schemaVersion,
    Long eventVersion,
    Instant occurredAt,
    String traceId,
    String aggregateType,
    String aggregateId,
    Long storeId,
    T data
) {}
