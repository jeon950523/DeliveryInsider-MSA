package com.deliveryinsider.order.domain.order.entity;

import com.deliveryinsider.order.domain.order.model.OutboxStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class OutboxEventEntity {

    private Long id;

    private String eventId;

    private String aggregateType;

    private String aggregateId;

    private String eventType;

    private int schemaVersion;

    private long eventVersion;

    private String payload;

    private String traceId;

    private OutboxStatus status;

    private int retryCount;

    private LocalDateTime nextRetryAt;

    private String lastErrorMessage;

    private String claimedBy;

    private LocalDateTime claimedUntil;

    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    @Builder
    public OutboxEventEntity(
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        int schemaVersion,
        long eventVersion,
        String payload,
        String traceId,
        OutboxStatus status
    ) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.schemaVersion = schemaVersion;
        this.eventVersion = eventVersion;
        this.payload = payload;
        this.traceId = traceId;
        this.status = status;
    }
}
