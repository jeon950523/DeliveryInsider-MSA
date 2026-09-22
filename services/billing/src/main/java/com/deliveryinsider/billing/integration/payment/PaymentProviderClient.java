package com.deliveryinsider.billing.integration.payment;

public interface PaymentProviderClient
    extends PaymentProviderQueryClient {

    PaymentProviderResult pay(
        String paymentOrderId,
        long amount,
        String idempotencyKey
    );
}
