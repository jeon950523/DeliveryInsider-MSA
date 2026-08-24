package com.deliveryinsider.platform.domain.webhook.provider.baemin;

public record BaeminOrderWebhookRequest(
    String sourceEventId,
    String eventType,
    String externalOrderId
) {
}
