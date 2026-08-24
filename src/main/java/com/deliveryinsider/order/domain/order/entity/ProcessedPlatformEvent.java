package com.deliveryinsider.order.domain.order.entity;

import com.deliveryinsider.order.domain.order.model.PlatformType;
import com.deliveryinsider.order.domain.order.model.ProcessedPlatformEventResult;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ProcessedPlatformEvent {

    private Long id;

    private PlatformType platformType;

    private String eventId;

    private String platformOrderId;

    private String eventType;

    private Long sourceSequence;

    private ProcessedPlatformEventResult processingResult;

    private LocalDateTime processedAt;

    private LocalDateTime createdAt;

    @Builder
    public ProcessedPlatformEvent(
        PlatformType platformType,
        String eventId,
        String platformOrderId,
        String eventType,
        Long sourceSequence,
        String processingResult
    ) {
        this.platformType = platformType;
        this.eventId = eventId;
        this.platformOrderId = platformOrderId;
        this.eventType = eventType;
        this.sourceSequence = sourceSequence;
        this.processingResult = ProcessedPlatformEventResult.valueOf(processingResult);
        this.createdAt = LocalDateTime.now();
    }
}
