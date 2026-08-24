package com.deliveryinsider.simulator.domain.baemin.webhook;

public record OrderWebhookEvent(
    String sourceEventId,
    String eventType,
    String externalOrderId
) {
}
