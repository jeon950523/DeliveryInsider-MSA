package com.deliveryinsider.billing.domain.payment.service;

import com.deliveryinsider.billing.domain.payment.entity.PaymentEntity;
import com.deliveryinsider.billing.domain.payment.mapper.PaymentMapper;
import com.deliveryinsider.billing.domain.payment.model.PaymentStatus;
import com.deliveryinsider.billing.domain.payment.model.PaymentType;
import com.deliveryinsider.billing.domain.subscription.entity.SubscriptionEntity;
import com.deliveryinsider.billing.domain.subscription.mapper.SubscriptionMapper;
import com.deliveryinsider.billing.domain.subscription.model.SubscriptionStatus;
import com.deliveryinsider.billing.global.error.BillingErrorCode;
import com.deliveryinsider.billing.global.error.BusinessException;
import com.deliveryinsider.billing.integration.payment.PaymentProviderResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InitialPaymentTransactionService {

    private final SubscriptionMapper subscriptionMapper;
    private final PaymentMapper paymentMapper;

    @Transactional
    public PaymentEntity prepare(
        Long storeId
    ) {
        SubscriptionEntity subscription =
            subscriptionMapper
                .findCurrentByStoreId(storeId)
                .orElseThrow(() ->
                    new BusinessException(
                        BillingErrorCode.SUBSCRIPTION_NOT_FOUND
                    )
                );

        subscription =
            subscriptionMapper
                .findByIdForUpdate(subscription.getId())
                .orElseThrow(() ->
                    new BusinessException(
                        BillingErrorCode.SUBSCRIPTION_NOT_FOUND
                    )
                );

        if (subscription.getStatus()
            != SubscriptionStatus.PENDING) {
            throw new BusinessException(
                BillingErrorCode.PAYMENT_STATE_CONFLICT
            );
        }

        if (paymentMapper
            .findInitialBySubscriptionId(
                subscription.getId()
            )
            .isPresent()) {

            throw new BusinessException(
                BillingErrorCode.PAYMENT_STATE_CONFLICT
            );
        }

        LocalDateTime now =
            LocalDateTime.now(
                ZoneOffset.UTC
            );

        PaymentEntity payment =
            PaymentEntity.builder()
                .subscriptionId(
                    subscription.getId()
                )
                .billingCycleKey("INITIAL")
                .attemptNo(1)
                .paymentType(
                    PaymentType.INITIAL
                )
                .paymentOrderId(
                    "DI-INITIAL-"
                        + UUID.randomUUID()
                )
                .idempotencyKey(
                    UUID.randomUUID().toString()
                )
                .provider("MOCK")
                .amount(
                    subscription.getBillingAmount()
                )
                .status(
                    PaymentStatus.REQUESTED
                )
                .requestedAt(now)
                .build();

        paymentMapper.insert(payment);

        return payment;
    }

    @Transactional
    public PaymentEntity succeed(
        Long paymentId,
        PaymentProviderResult providerResult
    ) {
        PaymentEntity payment =
            paymentMapper
                .findByIdForUpdate(paymentId)
                .orElseThrow(() ->
                    new BusinessException(
                        BillingErrorCode.PAYMENT_NOT_FOUND
                    )
                );

        if (payment.getStatus()
            != PaymentStatus.REQUESTED) {
            throw new BusinessException(
                BillingErrorCode.PAYMENT_STATE_CONFLICT
            );
        }

        SubscriptionEntity subscription =
            subscriptionMapper
                .findByIdForUpdate(
                    payment.getSubscriptionId()
                )
                .orElseThrow(() ->
                    new BusinessException(
                        BillingErrorCode.SUBSCRIPTION_NOT_FOUND
                    )
                );

        if (subscription.getStatus()
            != SubscriptionStatus.PENDING) {
            throw new BusinessException(
                BillingErrorCode.PAYMENT_STATE_CONFLICT
            );
        }

        LocalDateTime now =
            LocalDateTime.now(
                ZoneOffset.UTC
            );

        LocalDateTime periodStart =
            now;

        LocalDateTime periodEnd =
            periodStart.plusMonths(1);

        int paymentUpdated =
            paymentMapper.markSucceeded(
                payment.getId(),
                providerResult.providerPaymentKey(),
                periodStart,
                periodEnd,
                providerResult.methodType(),
                providerResult.cardCompany(),
                providerResult.cardNumberMasked(),
                now
            );

        if (paymentUpdated != 1) {
            throw new IllegalStateException(
                "Payment 성공 상태 변경에 실패했습니다."
            );
        }

        long nextVersion =
            subscription.getVersion() + 1;

        int subscriptionUpdated =
            subscriptionMapper.activate(
                subscription.getId(),
                now,
                periodStart,
                periodEnd,
                periodEnd,
                nextVersion
            );

        if (subscriptionUpdated != 1) {
            throw new IllegalStateException(
                "Subscription ACTIVE 전환에 실패했습니다."
            );
        }

        return paymentMapper
            .findByIdForUpdate(
                payment.getId()
            )
            .orElseThrow();
    }

    @Transactional
    public PaymentEntity fail(
        Long paymentId,
        PaymentProviderResult result
    ) {
        PaymentEntity payment =
            paymentMapper
                .findByIdForUpdate(paymentId)
                .orElseThrow(() ->
                    new BusinessException(
                        BillingErrorCode.PAYMENT_NOT_FOUND
                    )
                );

        paymentMapper.markFailed(
            paymentId,
            result.failureCode(),
            result.failureMessage(),
            LocalDateTime.now(ZoneOffset.UTC)
        );

        return paymentMapper
            .findByIdForUpdate(paymentId)
            .orElseThrow();
    }

    @Transactional
    public PaymentEntity unknown(
        Long paymentId,
        String errorMessage
    ) {
        PaymentEntity payment =
            paymentMapper
                .findByIdForUpdate(paymentId)
                .orElseThrow(() ->
                    new BusinessException(
                        BillingErrorCode.PAYMENT_NOT_FOUND
                    )
                );

        paymentMapper.markUnknown(
            paymentId,
            "PROVIDER_CALL_UNKNOWN",
            errorMessage
        );

        return paymentMapper
            .findByIdForUpdate(paymentId)
            .orElseThrow();
    }
}
