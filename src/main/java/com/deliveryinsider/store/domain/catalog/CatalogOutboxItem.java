package com.deliveryinsider.store.domain.catalog;

/** Publisher needs only routing/payload/ordering fields, not legacy JDBC datetime conversions. */
public record CatalogOutboxItem(Long id, String eventId, String topic, String kafkaKey, String payloadJson, Long eventVersion, int retryCount) {}
