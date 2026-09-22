package com.deliveryinsider.platform.domain.webhook.exception;

import lombok.Getter;

@Getter
public class BlockedWebhookProcessingException
    extends RuntimeException {

    private final String errorCode;

    public BlockedWebhookProcessingException(
        String errorCode,
        String message
    ) {
        super(message);
        this.errorCode = errorCode;
    }

    public BlockedWebhookProcessingException(
        String errorCode,
        String message,
        Throwable cause
    ) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
