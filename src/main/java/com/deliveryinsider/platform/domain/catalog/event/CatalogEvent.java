package com.deliveryinsider.platform.domain.catalog.event;

import java.time.Instant;

public record CatalogEvent(String eventId, String eventType, Integer schemaVersion, Long eventVersion, Instant occurredAt,
    String traceId, String aggregateType, String aggregateId, Long storeId, Data data) {
    public record Data(Long storeId, Long menuId, Long userId, String status, Instant deletedAt) {}
}
