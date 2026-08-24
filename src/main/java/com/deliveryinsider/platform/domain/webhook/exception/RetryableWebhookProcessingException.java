package com.deliveryinsider.platform.domain.webhook.exception;

import lombok.Getter;

@Getter
public class RetryableWebhookProcessingException
    extends RuntimeException {

    private final String errorCode;

    public RetryableWebhookProcessingException(
        String errorCode,
        String message
    ) {
        super(message);
        this.errorCode = errorCode;
    }

    public RetryableWebhookProcessingException(
        String errorCode,
        String message,
        Throwable cause
    ) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
