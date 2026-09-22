package com.deliveryinsider.platform.domain.webhook.entity;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ProviderWebhookInbox {

    private Long id;

    private PlatformType platformType;
    private String sourceEventId;
    private String eventType;
    private String externalOrderId;

    private String payloadJson;
    private String rawBodySha256;

    private ProviderWebhookInboxStatus status;

    private Integer retryCount;
    private LocalDateTime nextRetryAt;

    private String claimedBy;
    private LocalDateTime claimedUntil;
    private Long claimVersion;

    private Integer requeueCount;
    private LocalDateTime lastRequeuedAt;

    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;

    private String lastErrorCode;
    private String lastErrorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public ProviderWebhookInbox(
        PlatformType platformType,
        String sourceEventId,
        String eventType,
        String externalOrderId,
        String payloadJson,
        String rawBodySha256,
        ProviderWebhookInboxStatus status
    ) {
        this.platformType = platformType;
        this.sourceEventId = sourceEventId;
        this.eventType = eventType;
        this.externalOrderId = externalOrderId;
        this.payloadJson = payloadJson;
        this.rawBodySha256 = rawBodySha256;
        this.status = status;
    }
}
