package com.deliveryinsider.billing.integration.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPaymentProviderClient
    implements PaymentProviderClient {

    private static final String PROVIDER = "MOCK";

    private final MockPaymentMode mode;
    private final MockPaymentReconcileMode reconcileMode;

    public MockPaymentProviderClient(
        @Value("${billing.payment.mock.mode:SUCCESS}")
        MockPaymentMode mode,

        @Value("${billing.payment.mock.reconcile-mode:UNKNOWN}")
        MockPaymentReconcileMode reconcileMode
    ) {
        this.mode = mode;
        this.reconcileMode = reconcileMode;
    }

    @Override
    public String provider() {
        return PROVIDER;
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

    @Override
    public PaymentProviderResult findPayment(
        String paymentOrderId,
        long expectedAmount
    ) {
        return switch (reconcileMode) {

            case SUCCESS ->
                new PaymentProviderResult(
                    PaymentProviderResultStatus.SUCCEEDED,
                    "mock-reconciled-" + paymentOrderId,
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
                    null,
                    null,
                    null,
                    "MOCK_RECONCILED_FAILED",
                    "조회 결과 결제가 실패한 것으로 확인되었습니다."
                );

            case UNKNOWN ->
                new PaymentProviderResult(
                    PaymentProviderResultStatus.UNKNOWN,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "결제 결과를 아직 확인할 수 없습니다."
                );
        };
    }
}
