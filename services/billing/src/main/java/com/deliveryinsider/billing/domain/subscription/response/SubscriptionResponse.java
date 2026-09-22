package com.deliveryinsider.billing.domain.subscription.response;

import com.deliveryinsider.billing.domain.subscription.entity.SubscriptionEntity;

import java.time.LocalDateTime;

public record SubscriptionResponse(

    Long subscriptionId,
    Long storeId,
    Long planId,

    String status,

    long billingAmount,

    LocalDateTime startedAt,
    LocalDateTime currentPeriodStart,
    LocalDateTime currentPeriodEnd,
    LocalDateTime nextBillingAt,
    LocalDateTime pastDueAt,
    LocalDateTime canceledAt,
    LocalDateTime expiredAt,

    long version

) {

    public static SubscriptionResponse from(
        SubscriptionEntity subscription
    ) {
        return new SubscriptionResponse(
            subscription.getId(),
            subscription.getStoreId(),
            subscription.getPlanId(),
            subscription.getStatus().name(),
            subscription.getBillingAmount(),
            subscription.getStartedAt(),
            subscription.getCurrentPeriodStart(),
            subscription.getCurrentPeriodEnd(),
            subscription.getNextBillingAt(),
            subscription.getPastDueAt(),
            subscription.getCanceledAt(),
            subscription.getExpiredAt(),
            subscription.getVersion()
        );
    }
}
