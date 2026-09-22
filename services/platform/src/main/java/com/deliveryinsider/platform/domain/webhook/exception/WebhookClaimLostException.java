package com.deliveryinsider.platform.domain.webhook.exception;

public class WebhookClaimLostException extends RuntimeException {

    public WebhookClaimLostException() {
        super("Webhook inbox claim is no longer owned by this worker.");
    }
}
