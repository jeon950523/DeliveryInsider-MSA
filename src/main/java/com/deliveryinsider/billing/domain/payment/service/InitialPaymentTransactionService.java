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

    private static final String INITIAL_BILLING_CYCLE_KEY =
        "INITIAL";

    private static final String RENEWAL_BILLING_CYCLE_PREFIX =
        "RENEWAL-";

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
     * Toss 일반결제 준비.
     *
     * PENDING이면 최초 결제,
     * PAST_DUE이면 다음 이용기간 갱신 결제를 준비한다.
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
     * 구독 Payment 공통 준비 로직.
     *
     * - PENDING / PAST_DUE 구독만 결제 가능
     * - 같은 Billing Cycle에 REQUESTED / SUCCEEDED / UNKNOWN이 있으면 신규 결제 차단
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

        if (!isPayableStatus(
            subscription.getStatus()
        )) {
            throw new BusinessException(
                BillingErrorCode.PAYMENT_STATE_CONFLICT
            );
        }

        boolean initialPayment =
            subscription.getStatus()
                == SubscriptionStatus.PENDING;

        String billingCycleKey =
            resolveBillingCycleKey(
                subscription,
                initialPayment
            );

        var latestPayment =
            paymentMapper
                .findLatestBySubscriptionIdAndBillingCycleKey(
                    subscription.getId(),
                    billingCycleKey
                );

        if (latestPayment.isPresent()) {

            PaymentEntity existingPayment =
                latestPayment.get();

            PaymentStatus latestStatus =
                existingPayment.getStatus();

            if (latestStatus == PaymentStatus.REQUESTED) {

                if (provider.equals(
                    existingPayment.getProvider()
                )) {
                    return existingPayment;
                }

                throw new BusinessException(
                    BillingErrorCode.PAYMENT_STATE_CONFLICT
                );
            }

            if (latestStatus == PaymentStatus.SUCCEEDED
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
            resolvePaymentType(
                initialPayment,
                attemptNo
            );

        LocalDateTime now =
            LocalDateTime.now(
                ZoneOffset.UTC
            );

        String orderPrefix =
            initialPayment
                ? "DI-INITIAL-"
                : "DI-RENEW-";

        PaymentEntity payment =
            PaymentEntity.builder()
                .subscriptionId(
                    subscription.getId()
                )
                .billingCycleKey(
                    billingCycleKey
                )
                .attemptNo(
                    attemptNo
                )
                .paymentType(
                    paymentType
                )
                .paymentOrderId(
                    orderPrefix
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
     * 최초 결제:
     * PENDING -> ACTIVE
     *
     * 갱신 결제:
     * PAST_DUE -> ACTIVE
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

        if (!isPayableStatus(
            subscription.getStatus()
        )) {
            throw new BusinessException(
                BillingErrorCode.PAYMENT_STATE_CONFLICT
            );
        }

        SubscriptionStatus previousStatus =
            subscription.getStatus();

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
            previousStatus
                == SubscriptionStatus.PENDING
                ? subscriptionMapper.activate(
                    subscription.getId(),
                    now,
                    periodStart,
                    periodEnd,
                    periodEnd,
                    nextVersion
                )
                : subscriptionMapper.renew(
                    subscription.getId(),
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

        if (previousStatus
            == SubscriptionStatus.PENDING) {

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

        } else {

            billingOutboxWriter
                .appendSubscriptionRenewed(
                    subscription.getId(),
                    subscription.getStoreId(),
                    subscription.getPlanId(),
                    payment.getId(),
                    payment.getAmount(),
                    periodStart,
                    periodEnd,
                    nextVersion
                );
        }

        return paymentMapper
            .findByIdForUpdate(
                payment.getId()
            )
            .orElseThrow();
    }

    /**
     * 결제 실패 확정.
     *
     * FAILED는 같은 Billing Cycle에서
     * 이후 새로운 RETRY Payment를 생성할 수 있다.
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

        if (!isPayableStatus(
            subscription.getStatus()
        )) {
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

    private boolean isPayableStatus(
        SubscriptionStatus status
    ) {
        return status == SubscriptionStatus.PENDING
            || status == SubscriptionStatus.PAST_DUE;
    }

    private String resolveBillingCycleKey(
        SubscriptionEntity subscription,
        boolean initialPayment
    ) {
        if (initialPayment) {
            return INITIAL_BILLING_CYCLE_KEY;
        }

        return RENEWAL_BILLING_CYCLE_PREFIX
            + subscription.getVersion();
    }

    private PaymentType resolvePaymentType(
        boolean initialPayment,
        int attemptNo
    ) {
        if (attemptNo > 1) {
            return PaymentType.RETRY;
        }

        return initialPayment
            ? PaymentType.INITIAL
            : PaymentType.RECURRING;
    }
}
