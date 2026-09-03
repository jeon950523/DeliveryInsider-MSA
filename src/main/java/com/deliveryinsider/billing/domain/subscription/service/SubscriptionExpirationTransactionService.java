package com.deliveryinsider.billing.domain.subscription.service;

import com.deliveryinsider.billing.domain.outbox.service.BillingOutboxWriter;
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
    private final BillingOutboxWriter billingOutboxWriter;

    @Transactional
    public boolean expire(
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

        billingOutboxWriter
            .appendSubscriptionExpired(
                subscription.getId(),
                subscription.getStoreId(),
                subscription.getPlanId(),
                now,
                subscription.getCurrentPeriodEnd(),
                nextVersion
            );

        return true;
    }
}
