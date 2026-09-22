package com.deliveryinsider.notification.messaging.order.exception;

public class NonRetryableNotificationEventException
    extends RuntimeException {

    public NonRetryableNotificationEventException(
        String message
    ) {
        super(message);
    }

    public NonRetryableNotificationEventException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }
}
