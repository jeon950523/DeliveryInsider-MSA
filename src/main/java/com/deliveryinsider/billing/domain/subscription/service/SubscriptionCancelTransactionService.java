package com.deliveryinsider.billing.domain.subscription.service;

import com.deliveryinsider.billing.domain.outbox.service.BillingOutboxWriter;
import com.deliveryinsider.billing.domain.payment.mapper.PaymentMapper;
import com.deliveryinsider.billing.domain.subscription.entity.SubscriptionEntity;
import com.deliveryinsider.billing.domain.subscription.mapper.SubscriptionMapper;
import com.deliveryinsider.billing.domain.subscription.model.SubscriptionStatus;
import com.deliveryinsider.billing.domain.subscription.response.CancelSubscriptionResponse;
import com.deliveryinsider.billing.global.error.BillingErrorCode;
import com.deliveryinsider.billing.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class SubscriptionCancelTransactionService {

    private final SubscriptionMapper subscriptionMapper;
    private final PaymentMapper paymentMapper;
    private final BillingOutboxWriter billingOutboxWriter;

    @Transactional
    public CancelSubscriptionResponse cancel(
        Long storeId
    ) {
        SubscriptionEntity current =
            subscriptionMapper
                .findCurrentByStoreId(
                    storeId
                )
                .orElseThrow(() ->
                    new BusinessException(
                        BillingErrorCode.SUBSCRIPTION_NOT_FOUND
                    )
                );

        SubscriptionEntity subscription =
            subscriptionMapper
                .findByIdForUpdate(
                    current.getId()
                )
                .orElseThrow(() ->
                    new BusinessException(
                        BillingErrorCode.SUBSCRIPTION_NOT_FOUND
                    )
                );

        boolean financialInFlight =
            paymentMapper
                .existsFinancialInFlightBySubscriptionId(
                    subscription.getId()
                );

        if (financialInFlight) {
            throw new BusinessException(
                BillingErrorCode.FINANCIAL_IN_FLIGHT
            );
        }

        LocalDateTime now =
            LocalDateTime.now(
                ZoneOffset.UTC
            );

        long nextVersion =
            subscription.getVersion() + 1;

        if (subscription.getStatus()
            == SubscriptionStatus.ACTIVE) {

            return cancelActive(
                subscription,
                now,
                nextVersion
            );
        }

        if (subscription.getStatus()
            == SubscriptionStatus.PENDING
            || subscription.getStatus()
            == SubscriptionStatus.PAST_DUE) {

            return expireImmediately(
                subscription,
                now,
                nextVersion
            );
        }

        throw new BusinessException(
            BillingErrorCode.SUBSCRIPTION_CANCEL_NOT_ALLOWED
        );
    }

    private CancelSubscriptionResponse cancelActive(
        SubscriptionEntity subscription,
        LocalDateTime now,
        long nextVersion
    ) {
        int updated =
            subscriptionMapper.cancel(
                subscription.getId(),
                now,
                nextVersion
            );

        if (updated != 1) {
            throw new IllegalStateException(
                "Subscription CANCELED 전환에 실패했습니다."
            );
        }

        billingOutboxWriter
            .appendSubscriptionCanceled(
                subscription.getId(),
                subscription.getStoreId(),
                subscription.getPlanId(),
                now,
                subscription.getCurrentPeriodEnd(),
                nextVersion
            );

        return new CancelSubscriptionResponse(
            subscription.getId(),
            subscription.getStoreId(),
            SubscriptionStatus.CANCELED.name(),
            now,
            subscription.getCurrentPeriodEnd(),
            nextVersion
        );
    }

    private CancelSubscriptionResponse expireImmediately(
        SubscriptionEntity subscription,
        LocalDateTime now,
        long nextVersion
    ) {
        int updated =
            subscriptionMapper.expireByUserCancel(
                subscription.getId(),
                now,
                now,
                nextVersion
            );

        if (updated != 1) {
            throw new IllegalStateException(
                "Subscription EXPIRED 전환에 실패했습니다."
            );
        }

        billingOutboxWriter
            .appendSubscriptionExpired(
                subscription.getId(),
                subscription.getStoreId(),
                subscription.getPlanId(),
                now,
                subscription.getCurrentPeriodEnd(),
                nextVersion
            );

        return new CancelSubscriptionResponse(
            subscription.getId(),
            subscription.getStoreId(),
            SubscriptionStatus.EXPIRED.name(),
            now,
            subscription.getCurrentPeriodEnd(),
            nextVersion
        );
    }
}
