package com.deliveryinsider.billing.integration.event;

public record SubscriptionCanceledEventData(

    Long subscriptionId,
    Long planId,

    String canceledAt,
    String currentPeriodEnd

) {
}
