package com.deliveryinsider.simulator.domain.provider.webhook;

public record OrderWebhookEvent(
    String sourceEventId,
    String eventType,
    String externalOrderId
) {
}
