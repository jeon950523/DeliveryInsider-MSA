package com.deliveryinsider.store.global.outbox;

import java.time.Instant;

public record OutboxEvent(
    Long id,
    String eventId,
    String aggregateType,
    String aggregateId,
    Long eventVersion,
    String topic,
    String kafkaKey,
    String payloadJson,
    OutboxStatus status,
    int retryCount,
    Instant nextRetryAt,
    String claimedBy,
    Instant claimedUntil,
    Instant publishedAt,
    Instant createdAt
) {}
