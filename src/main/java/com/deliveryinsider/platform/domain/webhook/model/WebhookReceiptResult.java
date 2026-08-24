package com.deliveryinsider.platform.domain.webhook.model;

public record WebhookReceiptResult(
    Long inboxId,
    boolean duplicate
) {

    public static WebhookReceiptResult accepted(Long inboxId) {
        return new WebhookReceiptResult(
            inboxId,
            false
        );
    }

    public static WebhookReceiptResult duplicate(Long inboxId) {
        return new WebhookReceiptResult(
            inboxId,
            true
        );
    }
}
