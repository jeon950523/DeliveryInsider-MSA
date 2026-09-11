package com.deliveryinsider.platform.domain.webhook.entity;

public enum ProviderWebhookInboxStatus {
    RECEIVED,
    PROCESSING,
    PROCESSED,
    RETRYABLE_FAILED,
    BLOCKED
}
