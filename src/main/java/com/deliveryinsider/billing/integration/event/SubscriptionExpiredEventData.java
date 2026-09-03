package com.deliveryinsider.billing.integration.event;

public record SubscriptionExpiredEventData(

    Long subscriptionId,
    Long planId,

    String expiredAt,
    String currentPeriodEnd

) {
}
