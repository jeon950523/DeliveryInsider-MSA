package com.deliveryinsider.billing.domain.payment.response;

import com.deliveryinsider.billing.domain.payment.entity.PaymentEntity;

import java.time.LocalDateTime;

public record PaymentResponse(

    Long paymentId,
    Long subscriptionId,

    String paymentType,
    String status,

    long amount,

    String provider,
    String providerPaymentKey,

    LocalDateTime requestedAt,
    LocalDateTime approvedAt,

    String failureCode,
    String failureMessage

) {

    public static PaymentResponse from(
        PaymentEntity payment
    ) {
        return new PaymentResponse(
            payment.getId(),
            payment.getSubscriptionId(),
            payment.getPaymentType().name(),
            payment.getStatus().name(),
            payment.getAmount(),
            payment.getProvider(),
            payment.getProviderPaymentKey(),
            payment.getRequestedAt(),
            payment.getApprovedAt(),
            payment.getFailureCode(),
            payment.getFailureMessage()
        );
    }
}
