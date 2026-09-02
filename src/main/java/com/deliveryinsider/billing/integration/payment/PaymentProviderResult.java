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
}
