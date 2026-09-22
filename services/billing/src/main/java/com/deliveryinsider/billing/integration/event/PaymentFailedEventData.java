package com.deliveryinsider.billing.integration.event;

public record PaymentFailedEventData(

    Long paymentId,
    Long subscriptionId,

    int attemptNo,
    String paymentType,

    long amount,

    String failureCode,
    String failureMessage
) {
}
