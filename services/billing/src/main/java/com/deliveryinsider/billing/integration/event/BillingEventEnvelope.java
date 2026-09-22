package com.deliveryinsider.billing.integration.event;

public record BillingEventEnvelope<T>(

    String eventId,
    String eventType,

    int schemaVersion,
    long eventVersion,

    String occurredAt,
    String traceId,

    String aggregateType,
    String aggregateId,

    Long storeId,

    T data
) {
}
