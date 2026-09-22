package com.deliveryinsider.billing.domain.payment.response;

public record TossPaymentPrepareResponse(

    Long paymentId,
    Long subscriptionId,

    String orderId,
    String orderName,

    long amount

) {
}
