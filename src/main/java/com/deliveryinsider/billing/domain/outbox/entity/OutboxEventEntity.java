package com.deliveryinsider.billing.domain.outbox.entity;

import com.deliveryinsider.billing.domain.outbox.model.OutboxStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEventEntity {

    private Long id;

    private String eventId;

    private String aggregateType;
    private String aggregateId;

    private String eventType;

    private int schemaVersion;
    private long eventVersion;

    private String topic;
    private String kafkaKey;
    private String payloadJson;

    private String traceId;

    private OutboxStatus status;

    private int retryCount;
    private LocalDateTime nextRetryAt;

    private String claimedBy;
    private LocalDateTime claimedUntil;

    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}
