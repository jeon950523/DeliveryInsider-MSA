package com.deliveryinsider.platform.domain.webhook.model;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import lombok.Builder;

@Builder
public record ClaimedWebhook(
    Long inboxId,
    PlatformType platformType,
    String sourceEventId,
    String eventType,
    String externalOrderId,
    String payloadJson,
    long claimVersion
) {
}
