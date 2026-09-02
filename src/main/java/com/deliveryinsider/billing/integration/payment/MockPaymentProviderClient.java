package com.deliveryinsider.billing.integration.payment;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPaymentProviderClient
    implements PaymentProviderClient {

    @Override
    public PaymentProviderResult pay(
        String paymentOrderId,
        long amount,
        String idempotencyKey
    ) {
        return new PaymentProviderResult(
            PaymentProviderResultStatus.SUCCEEDED,
            "mock-payment-" + UUID.randomUUID(),
            "CARD",
            "DELIVERY_TEST_CARD",
            "****-****-****-1234",
            null,
            null
        );
    }
}
