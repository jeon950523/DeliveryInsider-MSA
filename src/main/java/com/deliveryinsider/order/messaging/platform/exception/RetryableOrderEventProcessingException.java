package com.deliveryinsider.order.messaging.platform.exception;

import lombok.Getter;

@Getter
public class RetryableOrderEventProcessingException
    extends RuntimeException {

    private final String errorCode;

    public RetryableOrderEventProcessingException(
        String errorCode,
        String message,
        Throwable cause
    ) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
