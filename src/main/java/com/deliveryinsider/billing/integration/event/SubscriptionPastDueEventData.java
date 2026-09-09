package com.deliveryinsider.billing.integration.event;

public record SubscriptionPastDueEventData(

    Long subscriptionId,
    Long planId,

    String pastDueAt,
    String currentPeriodEnd

) {
}
