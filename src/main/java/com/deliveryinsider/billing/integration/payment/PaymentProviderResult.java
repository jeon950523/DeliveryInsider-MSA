package com.deliveryinsider.billing.integration.payment;

public record PaymentProviderResult(

    PaymentProviderResultStatus status,

    String providerPaymentKey,

    String methodType,
    String cardCompany,
    String cardNumberMasked,

    String failureCode,
    String failureMessage

) {
    public PaymentProviderResult {
        if (failureCode != null || failureMessage != null) {
            var safe = PaymentFailureDetails.from(failureCode, failureMessage);
            failureCode = safe.code();
            failureMessage = safe.message();
        }
    }
}
