package com.deliveryinsider.report.messaging.order.exception;

public class NonRetryableReportEventException
    extends RuntimeException {

    public NonRetryableReportEventException(
        String message
    ) {
        super(message);
    }

    public NonRetryableReportEventException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }
}
