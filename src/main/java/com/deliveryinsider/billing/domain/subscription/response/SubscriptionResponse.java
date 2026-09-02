package com.deliveryinsider.billing.domain.subscription.response;

import com.deliveryinsider.billing.domain.subscription.entity.SubscriptionEntity;

public record SubscriptionResponse(
    Long subscriptionId,
    Long storeId,
    Long planId,
    String status,
    long billingAmount,
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
            subscription.getVersion()
        );
    }
}
