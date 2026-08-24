package com.deliveryinsider.order.messaging.platform.exception;

import lombok.Getter;

@Getter
public class NonRetryableOrderEventProcessingException
    extends RuntimeException {

    private final String errorCode;

    public NonRetryableOrderEventProcessingException(
        String errorCode,
        String message
    ) {
        super(message);
        this.errorCode = errorCode;
    }

    public NonRetryableOrderEventProcessingException(
        String errorCode,
        String message,
        Throwable cause
    ) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
