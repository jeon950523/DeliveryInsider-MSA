package com.deliveryinsider.billing.integration.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPaymentProviderClient
    implements PaymentProviderClient {

    private final MockPaymentMode mode;

    public MockPaymentProviderClient(
        @Value("${billing.payment.mock.mode:SUCCESS}")
        MockPaymentMode mode
    ) {
        this.mode = mode;
    }

    @Override
    public PaymentProviderResult pay(
        String paymentOrderId,
        long amount,
        String idempotencyKey
    ) {
        return switch (mode) {

            case SUCCESS ->
                new PaymentProviderResult(
                    PaymentProviderResultStatus.SUCCEEDED,
                    "mock-payment-" + UUID.randomUUID(),
                    "CARD",
                    "DELIVERY_TEST_CARD",
                    "****-****-****-1234",
                    null,
                    null
                );

            case FAIL ->
                new PaymentProviderResult(
                    PaymentProviderResultStatus.FAILED,
                    null,
                    "CARD",
                    "DELIVERY_TEST_CARD",
                    "****-****-****-1234",
                    "MOCK_CARD_DECLINED",
                    "Mock 카드 결제가 거절되었습니다."
                );

            case TIMEOUT ->
                throw new IllegalStateException(
                    "Mock Payment Provider timeout"
                );
        };
    }
}
