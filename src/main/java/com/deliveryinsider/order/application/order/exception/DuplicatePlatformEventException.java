package com.deliveryinsider.order.application.order.exception;

public class DuplicatePlatformEventException
    extends RuntimeException {

    public DuplicatePlatformEventException(
        String eventId,
        Throwable cause
    ) {
        super(
            "이미 처리된 Platform Event입니다. eventId="
                + eventId,
            cause
        );
    }
}
