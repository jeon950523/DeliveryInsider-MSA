package com.deliveryinsider.platform.domain.webhook.model;

public record WebhookReceiptCommand(
    String sourceEventId,
    String eventType,
    String externalOrderId,
    String payloadJson
) {
}
