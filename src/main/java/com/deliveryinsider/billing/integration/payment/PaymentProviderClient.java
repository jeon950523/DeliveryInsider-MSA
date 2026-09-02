package com.deliveryinsider.billing.integration.payment;

public interface PaymentProviderClient {

    PaymentProviderResult pay(
        String paymentOrderId,
        long amount,
        String idempotencyKey
    );

    PaymentProviderResult findPayment(
        String paymentOrderId
    );
}
