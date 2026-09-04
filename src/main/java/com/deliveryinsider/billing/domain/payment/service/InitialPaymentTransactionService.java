package com.deliveryinsider.billing.domain.payment.service;

import com.deliveryinsider.billing.domain.outbox.service.BillingOutboxWriter;
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

    private static final String INITIAL_BILLING_CYCLE_KEY = "INITIAL";

    private final SubscriptionMapper subscriptionMapper;
    private final PaymentMapper paymentMapper;
    private final BillingOutboxWriter billingOutboxWriter;

    /**
     * Mock 결제 회귀 테스트용 Payment 준비.
     */
    @Transactional
    public PaymentEntity prepare(
        Long storeId
    ) {
        return prepareInternal(
            storeId,
            "MOCK"
        );
    }

    /**
     * 실제 Toss 일반결제 준비.
     */
    @Transactional
    public PaymentEntity prepareToss(
        Long storeId
    ) {
        return prepareInternal(
            storeId,
            "TOSS"
        );
    }

    /**
     * Initial Payment 공통 준비 로직.
     *
     * - PENDING 구독만 결제 가능
     * - REQUESTED / SUCCEEDED / UNKNOWN 존재 시 신규 결제 차단
     * - FAILED 이후에는 RETRY Payment 생성 가능
     */
    private PaymentEntity prepareInternal(
        Long storeId,
        String provider
    ) {
        SubscriptionEntity subscription =
            subscriptionMapper
                .findCurrentByStoreId(
                    storeId
                )
                .orElseThrow(() ->
                    new BusinessException(
                        BillingErrorCode.SUBSCRIPTION_NOT_FOUND
                    )
                );

        subscription =
            subscriptionMapper
                .findByIdForUpdate(
                    subscription.getId()
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

        var latestPayment =
            paymentMapper
                .findLatestBySubscriptionIdAndBillingCycleKey(
                    subscription.getId(),
                    INITIAL_BILLING_CYCLE_KEY
                );

        if (latestPayment.isPresent()) {

            PaymentStatus latestStatus =
                latestPayment
                    .get()
                    .getStatus();

            if (latestStatus == PaymentStatus.REQUESTED
                || latestStatus == PaymentStatus.SUCCEEDED
                || latestStatus == PaymentStatus.UNKNOWN) {

                throw new BusinessException(
                    BillingErrorCode.PAYMENT_STATE_CONFLICT
                );
            }
        }

        int attemptNo =
            latestPayment
                .map(PaymentEntity::getAttemptNo)
                .map(value -> value + 1)
                .orElse(1);

        PaymentType paymentType =
            attemptNo == 1
                ? PaymentType.INITIAL
                : PaymentType.RETRY;

        LocalDateTime now =
            LocalDateTime.now(
                ZoneOffset.UTC
            );

        PaymentEntity payment =
            PaymentEntity.builder()
                .subscriptionId(
                    subscription.getId()
                )
                .billingCycleKey(
                    INITIAL_BILLING_CYCLE_KEY
                )
                .attemptNo(
                    attemptNo
                )
                .paymentType(
                    paymentType
                )
                .paymentOrderId(
                    "DI-INITIAL-"
                        + subscription.getId()
                        + "-"
                        + attemptNo
                        + "-"
                        + UUID.randomUUID()
                )
                .idempotencyKey(
                    UUID.randomUUID().toString()
                )
                .provider(
                    provider
                )
                .amount(
                    subscription.getBillingAmount()
                )
                .status(
                    PaymentStatus.REQUESTED
                )
                .requestedAt(
                    now
                )
                .build();

        int inserted =
            paymentMapper.insert(
                payment
            );

        if (inserted != 1) {
            throw new IllegalStateException(
                "Payment REQUESTED 생성에 실패했습니다."
            );
        }

        return payment;
    }

    /**
     * 결제 성공 확정.
     *
     * REQUESTED 또는 UNKNOWN Payment를 SUCCEEDED로 확정하고
     * PENDING Subscription을 ACTIVE로 전환한다.
     */
    @Transactional
    public PaymentEntity succeed(
        Long paymentId,
        PaymentProviderResult providerResult
    ) {
        PaymentEntity payment =
            paymentMapper
                .findByIdForUpdate(
                    paymentId
                )
                .orElseThrow(() ->
                    new BusinessException(
                        BillingErrorCode.PAYMENT_NOT_FOUND
                    )
                );

        if (payment.getStatus() != PaymentStatus.REQUESTED
            && payment.getStatus() != PaymentStatus.UNKNOWN) {

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

        billingOutboxWriter
            .appendSubscriptionActivated(
                subscription.getId(),
                subscription.getStoreId(),
                subscription.getPlanId(),
                payment.getId(),
                payment.getAmount(),
                periodStart,
                periodEnd,
                nextVersion
            );

        return paymentMapper
            .findByIdForUpdate(
                payment.getId()
            )
            .orElseThrow();
    }

    /**
     * 결제 실패 확정.
     *
     * FAILED는 이후 새로운 RETRY Payment를 생성할 수 있다.
     */
    @Transactional
    public PaymentEntity fail(
        Long paymentId,
        PaymentProviderResult result
    ) {
        PaymentEntity payment =
            paymentMapper
                .findByIdForUpdate(
                    paymentId
                )
                .orElseThrow(() ->
                    new BusinessException(
                        BillingErrorCode.PAYMENT_NOT_FOUND
                    )
                );

        if (payment.getStatus() != PaymentStatus.REQUESTED
            && payment.getStatus() != PaymentStatus.UNKNOWN) {

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

        int updated =
            paymentMapper.markFailed(
                paymentId,
                result.failureCode(),
                result.failureMessage(),
                LocalDateTime.now(
                    ZoneOffset.UTC
                )
            );

        if (updated != 1) {
            throw new IllegalStateException(
                "Payment 실패 상태 변경에 실패했습니다."
            );
        }

        billingOutboxWriter
            .appendPaymentFailed(
                payment.getId(),
                payment.getSubscriptionId(),
                subscription.getStoreId(),
                payment.getAttemptNo(),
                payment.getPaymentType().name(),
                payment.getAmount(),
                result.failureCode(),
                result.failureMessage()
            );

        return paymentMapper
            .findByIdForUpdate(
                paymentId
            )
            .orElseThrow();
    }

    /**
     * 결제 결과를 확정할 수 없는 경우.
     *
     * REQUESTED -> UNKNOWN
     *
     * 실제 결제가 성공했을 가능성이 있으므로
     * FAILED로 단정하지 않고 Reconciliation 대상으로 남긴다.
     */
    @Transactional
    public PaymentEntity unknown(
        Long paymentId,
        String errorMessage
    ) {
        PaymentEntity payment =
            paymentMapper
                .findByIdForUpdate(
                    paymentId
                )
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

        int updated =
            paymentMapper.markUnknown(
                paymentId,
                "PROVIDER_CALL_UNKNOWN",
                errorMessage
            );

        if (updated != 1) {
            throw new IllegalStateException(
                "Payment UNKNOWN 상태 변경에 실패했습니다."
            );
        }

        return paymentMapper
            .findByIdForUpdate(
                paymentId
            )
            .orElseThrow();
    }
}
