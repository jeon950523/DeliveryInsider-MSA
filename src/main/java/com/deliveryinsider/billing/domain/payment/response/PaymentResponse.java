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
            payment.getFailureMessage() == null ? null
                : "UNKNOWN".equals(payment.getStatus().name())
                    ? "결제 결과를 확인 중입니다. 잠시 후 구독 상태를 확인해 주세요."
                    : "결제가 완료되지 않았습니다. 결제 수단을 확인하고 다시 시도해 주세요."
        );
    }
}
