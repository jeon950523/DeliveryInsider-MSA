package com.deliveryinsider.billing.integration.payment;

public interface PaymentProviderQueryClient {

    String provider();

    PaymentProviderResult findPayment(
        String paymentOrderId,
        long expectedAmount
    );
}
