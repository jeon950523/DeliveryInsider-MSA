package com.deliveryinsider.report.messaging.order.exception;

public class RetryableReportEventException
    extends RuntimeException {

    public RetryableReportEventException(
        String message
    ) {
        super(message);
    }
}
