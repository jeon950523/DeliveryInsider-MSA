package com.deliveryinsider.billing.domain.subscription.service;

import com.deliveryinsider.billing.domain.outbox.service.BillingOutboxWriter;
import com.deliveryinsider.billing.domain.payment.mapper.PaymentMapper;
import com.deliveryinsider.billing.domain.subscription.entity.SubscriptionEntity;
import com.deliveryinsider.billing.domain.subscription.mapper.SubscriptionMapper;
import com.deliveryinsider.billing.domain.subscription.model.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriptionExpirationTransactionService {

    private final SubscriptionMapper subscriptionMapper;
    private final PaymentMapper paymentMapper;
    private final BillingOutboxWriter billingOutboxWriter;

    @Transactional
    public boolean expireCanceled(
        Long subscriptionId,
        LocalDateTime now
    ) {
        SubscriptionEntity subscription =
            subscriptionMapper
                .findByIdForUpdate(
                    subscriptionId
                )
                .orElse(null);

        if (subscription == null) {
            return false;
        }

        if (subscription.getStatus()
            != SubscriptionStatus.CANCELED) {
            return false;
        }

        if (subscription.getCurrentPeriodEnd() == null
            || subscription
                .getCurrentPeriodEnd()
                .isAfter(now)) {
            return false;
        }

        if (paymentMapper
            .existsFinancialInFlightBySubscriptionId(
                subscription.getId()
            )) {
            return false;
        }

        long nextVersion =
            subscription.getVersion() + 1;

        int updated =
            subscriptionMapper.expire(
                subscription.getId(),
                now,
                nextVersion
            );

        if (updated != 1) {
            throw new IllegalStateException(
                "Subscription EXPIRED 전환에 실패했습니다."
            );
        }

        appendExpired(
            subscription,
            now,
            nextVersion
        );

        return true;
    }

    @Transactional
    public boolean expirePastDue(
        Long subscriptionId,
        LocalDateTime now,
        LocalDateTime cutoff
    ) {
        SubscriptionEntity subscription =
            subscriptionMapper
                .findByIdForUpdate(
                    subscriptionId
                )
                .orElse(null);

        if (subscription == null) {
            return false;
        }

        if (subscription.getStatus()
            != SubscriptionStatus.PAST_DUE) {
            return false;
        }

        if (subscription.getPastDueAt() == null
            || subscription
                .getPastDueAt()
                .isAfter(cutoff)) {
            return false;
        }

        if (paymentMapper
            .existsFinancialInFlightBySubscriptionId(
                subscription.getId()
            )) {
            return false;
        }

        long nextVersion =
            subscription.getVersion() + 1;

        int updated =
            subscriptionMapper.expirePastDue(
                subscription.getId(),
                cutoff,
                now,
                nextVersion
            );

        if (updated != 1) {
            throw new IllegalStateException(
                "PAST_DUE Subscription EXPIRED 전환에 실패했습니다."
            );
        }

        appendExpired(
            subscription,
            now,
            nextVersion
        );

        return true;
    }

    private void appendExpired(
        SubscriptionEntity subscription,
        LocalDateTime now,
        long nextVersion
    ) {
        billingOutboxWriter
            .appendSubscriptionExpired(
                subscription.getId(),
                subscription.getStoreId(),
                subscription.getPlanId(),
                now,
                subscription.getCurrentPeriodEnd(),
                nextVersion
            );
    }
}
