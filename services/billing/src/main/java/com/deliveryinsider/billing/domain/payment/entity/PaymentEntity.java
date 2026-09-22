package com.deliveryinsider.billing.domain.payment.entity;

import com.deliveryinsider.billing.domain.payment.model.PaymentStatus;
import com.deliveryinsider.billing.domain.payment.model.PaymentType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class PaymentEntity {

    private Long id;

    private Long subscriptionId;

    private String billingCycleKey;
    private int attemptNo;

    private PaymentType paymentType;

    private String paymentOrderId;
    private String idempotencyKey;

    private String provider;
    private String providerPaymentKey;

    private long amount;
    private PaymentStatus status;

    private LocalDateTime servicePeriodStart;
    private LocalDateTime servicePeriodEnd;

    private String methodTypeSnapshot;
    private String cardCompanySnapshot;
    private String cardNumberMaskedSnapshot;

    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime failedAt;

    private String failureCode;
    private String failureMessage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public PaymentEntity(
        Long subscriptionId,
        String billingCycleKey,
        int attemptNo,
        PaymentType paymentType,
        String paymentOrderId,
        String idempotencyKey,
        String provider,
        long amount,
        PaymentStatus status,
        LocalDateTime requestedAt
    ) {
        this.subscriptionId = subscriptionId;
        this.billingCycleKey = billingCycleKey;
        this.attemptNo = attemptNo;
        this.paymentType = paymentType;
        this.paymentOrderId = paymentOrderId;
        this.idempotencyKey = idempotencyKey;
        this.provider = provider;
        this.amount = amount;
        this.status = status;
        this.requestedAt = requestedAt;
    }
}
