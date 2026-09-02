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

    private static final String INITIAL_BILLING_CYCLE_KEY = "INITIAL";

    private final SubscriptionMapper subscriptionMapper;
    private final PaymentMapper paymentMapper;

    /**
     * 초기 결제 시도 준비.
     *
     * 1. 현재 구독 조회
     * 2. Subscription row lock
     * 3. PENDING 상태 확인
     * 4. 같은 INITIAL 결제 주기의 마지막 Payment 확인
     * 5. FAILED인 경우에만 재시도 허용
     * 6. 새로운 REQUESTED Payment 생성
     */
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
                .attemptNo(attemptNo)
                .paymentType(paymentType)
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

    /**
     * PG 성공 결과 확정.
     *
     * Payment REQUESTED -> SUCCEEDED
     * Subscription PENDING -> ACTIVE
     *
     * 두 변경은 같은 DB Transaction으로 처리한다.
     */
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

    /**
     * PG가 명확한 결제 실패를 반환한 경우.
     *
     * Payment만 FAILED로 변경하고
     * Subscription은 PENDING으로 유지한다.
     *
     * FAILED는 이후 재시도가 가능하다.
     */
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

        if (payment.getStatus()
            != PaymentStatus.REQUESTED) {

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

        return paymentMapper
            .findByIdForUpdate(paymentId)
            .orElseThrow();
    }

    /**
     * Timeout 등으로 PG 결제 결과를 알 수 없는 경우.
     *
     * 실제 결제가 성공했을 가능성이 있기 때문에
     * FAILED가 아니라 UNKNOWN으로 기록한다.
     *
     * UNKNOWN 상태에서는 자동 재결제를 허용하지 않는다.
     */
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
            .findByIdForUpdate(paymentId)
            .orElseThrow();
    }
}
